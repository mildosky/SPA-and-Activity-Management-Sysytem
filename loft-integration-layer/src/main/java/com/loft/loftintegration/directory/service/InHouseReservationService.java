package com.loft.loftintegration.directory.service;

import com.loft.loftintegration.directory.model.InHouseReservationMirror;
import com.loft.loftintegration.directory.repository.InHouseReservationMirrorRepository;
import com.loft.loftintegration.sync.model.ReservationEvent;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Keeps a local mirror of Opera's IN-HOUSE reservations, populated by
 * SyncEngine from every ReservationEvent it pulls. This is what makes
 * "charge to the room" real: a kiosk booking for a checked-in guest can
 * be resolved to that guest's CURRENT stay (RESV_NAME_ID), which is the
 * exact key BillingService needs to post the charge onto their Opera
 * folio.
 *
 * Upserts by operaReservationId. CANCELLED events remove the row. Rows
 * whose departure date has passed are pruned on each sync pass, so the
 * mirror only ever contains guests who are actually in-house today —
 * a booking for yesterday's checkout must never get posted to a closed
 * folio.
 */
@Service
public class InHouseReservationService {

    private final InHouseReservationMirrorRepository repository;
    private final GuestDirectoryService guestDirectoryService;

    public InHouseReservationService(InHouseReservationMirrorRepository repository,
                                      GuestDirectoryService guestDirectoryService) {
        this.repository = repository;
        this.guestDirectoryService = guestDirectoryService;
    }

    @Transactional
    public void upsert(ReservationEvent event) {
        if (event.getChangeType() == ReservationEvent.ChangeType.CANCELLED) {
            repository.findByOperaReservationId(event.getReservationId())
                    .ifPresent(repository::delete);
            return;
        }

        // The reservation event carries NAME_ID but not the guest's name —
        // enrich from the profile mirror when available so the kiosk picker
        // can show "Jane Smith — Room 412".
        String firstName = null;
        String lastName = null;
        Optional<String> guestName = guestDirectoryService.findGuestNameById(event.getGuestProfileId());
        if (guestName.isPresent()) {
            String[] parts = guestName.get().split("\\s+", 2);
            firstName = parts[0];
            lastName = parts.length > 1 ? parts[1] : null;
        }

        Optional<InHouseReservationMirror> existing =
                repository.findByOperaReservationId(event.getReservationId());
        if (existing.isPresent()) {
            existing.get().updateFrom(event.getGuestProfileId(), firstName, lastName,
                    event.getRoomNumber(), event.getArrivalDate(), event.getDepartureDate());
            repository.save(existing.get());
        } else {
            repository.save(new InHouseReservationMirror(event.getReservationId(),
                    event.getGuestProfileId(), event.getPropertyCode(), firstName, lastName,
                    event.getRoomNumber(), event.getArrivalDate(), event.getDepartureDate()));
        }
    }

    /** Prune rows whose stay has ended (departure before today). Called after each poll pass. */
    @Transactional
    public void pruneCheckedOut() {
        LocalDate today = LocalDate.now();
        List<InHouseReservationMirror> stale = repository.findAll().stream()
                .filter(m -> m.getDepartureDate() == null || m.getDepartureDate().isBefore(today))
                .toList();
        if (!stale.isEmpty()) {
            repository.deleteAll(stale);
        }
    }

    /** All guests currently checked in at this property — drives the kiosk "I'm staying here" picker. */
    @Transactional(readOnly = true)
    public List<InHouseReservationMirror> findInHouseGuests(String propertyCode) {
        LocalDate today = LocalDate.now();
        return repository.findByPropertyCode(propertyCode).stream()
                .filter(m -> m.isCurrentlyInHouse(today))
                .sorted((a, b) -> String.valueOf(a.getLastName()).compareToIgnoreCase(String.valueOf(b.getLastName())))
                .toList();
    }

    @Transactional(readOnly = true)
    public Optional<InHouseReservationMirror> findByGuestProfileId(String guestProfileId) {
        LocalDate today = LocalDate.now();
        return repository.findByGuestProfileId(guestProfileId)
                .filter(m -> m.isCurrentlyInHouse(today));
    }

    @Transactional(readOnly = true)
    public Optional<InHouseReservationMirror> findByOperaReservationId(String operaReservationId) {
        return repository.findByOperaReservationId(operaReservationId);
    }
}
