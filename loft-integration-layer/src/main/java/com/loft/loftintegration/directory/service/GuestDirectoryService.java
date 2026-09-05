package com.loft.loftintegration.directory.service;

import com.loft.loftintegration.directory.model.OperaGuestMirror;
import com.loft.loftintegration.directory.repository.OperaGuestMirrorRepository;
import com.loft.loftintegration.sync.model.GuestProfileEvent;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Keeps the local guest directory mirror in sync with Opera, and
 * answers "is there a known Opera guest with this email" for the
 * webshop's guest-linking feature.
 *
 * Note on real-world matching odds: in the George Lagos lab, only 1 of
 * 63 guest profiles has an email on file (see NAME_PHONE PHONE_TYPE='EMAIL'
 * discovery from earlier testing) — so most webshop bookings won't find
 * a match here even when this is working perfectly. That's a data
 * population gap in the source system, not a bug in this matching
 * logic. A real production Opera install with better email capture at
 * check-in would match far more often.
 */
@Service
public class GuestDirectoryService {

    private final OperaGuestMirrorRepository repository;

    public GuestDirectoryService(OperaGuestMirrorRepository repository) {
        this.repository = repository;
    }

    /**
     * Called by SyncEngine for every GuestProfileEvent it pulls from
     * Opera. Upserts by operaNameId — updates the existing mirror row
     * if one exists (guest profile was modified), inserts a new one
     * otherwise (new guest profile, or first time we've seen this one
     * since this mirror table was created).
     */
    @Transactional
    public void upsert(GuestProfileEvent event) {
        Optional<OperaGuestMirror> existing = repository.findByOperaNameId(event.getGuestProfileId());

        if (existing.isPresent()) {
            OperaGuestMirror mirror = existing.get();
            mirror.updateFrom(event.getPropertyCode(), event.getFirstName(), event.getLastName(),
                    event.getEmail(), event.getPhone());
            repository.save(mirror);
        } else {
            repository.save(new OperaGuestMirror(
                    event.getGuestProfileId(), event.getPropertyCode(),
                    event.getFirstName(), event.getLastName(), event.getEmail(), event.getPhone()));
        }
    }

    /**
     * Looks for a known Opera guest by email. Returns the Opera NAME_ID
     * if found, empty otherwise (no match, or email is null/blank —
     * matching on a blank email would be meaningless and dangerous,
     * since multiple mirror rows could plausibly have null emails).
     */
    @Transactional(readOnly = true)
    public Optional<String> findOperaNameIdByEmail(String email) {
        if (email == null || email.isBlank()) {
            return Optional.empty();
        }
        return repository.findByEmailIgnoreCase(email).map(OperaGuestMirror::getOperaNameId);
    }
}
