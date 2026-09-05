package com.loft.loftintegration.pos.service;

import com.loft.loftintegration.booking.model.Booking;
import com.loft.loftintegration.connector.PmsConnectionException;
import com.loft.loftintegration.pos.model.Charge;
import com.loft.loftintegration.pos.model.ChargeStatus;
import com.loft.loftintegration.pos.model.ChargeType;
import com.loft.loftintegration.pos.model.PosSale;
import com.loft.loftintegration.pos.repository.ChargeRepository;
import com.loft.loftintegration.sync.SyncEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Creates Charges from completed Bookings and finalized PosSales, and
 * — when enabled — posts them to Opera's folio via SyncEngine.
 *
 * Opera folio posting is OFF by default
 * (tac-integration.opera-folio-posting-enabled: false in
 * application.yml) because OperaV5DirectConnector.postFolioCharge()
 * is built from UNVERIFIED column values (see its class doc) — this
 * flag exists specifically so nothing writes to Opera's live database
 * until those values are confirmed against a real install. When
 * disabled, or when a charge has no operaReservationId to post
 * against, charges are simply marked POSTED_LOCALLY instead — the
 * charge still exists and is billed within this system, just not
 * reflected on the guest's Opera room folio.
 */
@Service
public class BillingService {

    private static final Logger log = LoggerFactory.getLogger(BillingService.class);

    private final ChargeRepository chargeRepository;
    private final SyncEngine syncEngine;
    private final boolean operaFolioPostingEnabled;

    public BillingService(ChargeRepository chargeRepository, SyncEngine syncEngine,
                           @Value("${tac-integration.opera-folio-posting-enabled:false}") boolean operaFolioPostingEnabled) {
        this.chargeRepository = chargeRepository;
        this.syncEngine = syncEngine;
        this.operaFolioPostingEnabled = operaFolioPostingEnabled;
    }

    /** Creates and (attempts to) post a Charge for a completed Booking, priced from its ActivityType. */
    @Transactional
    public Charge chargeForBooking(Booking booking) {
        Charge charge = new Charge(
                booking.getPropertyCode(),
                booking.getGuestProfileId(),
                booking.getOperaReservationId(),
                booking.getActivityType().getPrice(),
                booking.getActivityType().getCurrency(),
                booking.getActivityType().getName(),
                ChargeType.BOOKING,
                booking.getId());

        charge = chargeRepository.save(charge);
        attemptPost(charge);
        return charge;
    }

    /** Creates and (attempts to) post a Charge for a finalized PosSale, priced from its line items' total. */
    @Transactional
    public Charge chargeForPosSale(PosSale sale) {
        Charge charge = new Charge(
                sale.getPropertyCode(),
                sale.getGuestProfileId(),
                sale.getOperaReservationId(),
                sale.total(),
                sale.getCurrency(),
                "Retail sale (" + sale.getLineItems().size() + " item(s))",
                ChargeType.RETAIL_SALE,
                sale.getId());

        charge = chargeRepository.save(charge);
        attemptPost(charge);
        return charge;
    }

    /**
     * Tries to post a charge to Opera if posting is enabled AND the
     * charge has a real Opera reservation to post against. Falls back
     * to POSTED_LOCALLY otherwise — a charge is never left stuck in
     * PENDING just because Opera posting isn't applicable or fails;
     * the guest still gets billed within this system either way.
     */
    private void attemptPost(Charge charge) {
        if (!operaFolioPostingEnabled) {
            log.debug("Opera folio posting disabled — charge {} marked POSTED_LOCALLY.", charge.getId());
            markPostedLocally(charge);
            return;
        }

        if (charge.getOperaReservationId() == null || !syncEngine.hasProperty(charge.getPropertyCode())) {
            log.debug("Charge {} has no Opera reservation link (or property not connected) — marked POSTED_LOCALLY.",
                    charge.getId());
            markPostedLocally(charge);
            return;
        }

        try {
            syncEngine.postFolioCharge(charge.getPropertyCode(), charge.getOperaReservationId(),
                    charge.getAmount(), charge.getCurrency(), charge.getDescription());
            charge.setStatus(ChargeStatus.POSTED_TO_OPERA);
            charge.setPostedAt(java.time.LocalDateTime.now());
            chargeRepository.save(charge);
            log.info("Charge {} posted to Opera folio for reservation {}.",
                    charge.getId(), charge.getOperaReservationId());
        } catch (PmsConnectionException e) {
            // Posting failed — leave PENDING rather than silently
            // falling back to POSTED_LOCALLY. A failed Opera post is a
            // real problem someone should notice and retry, not quietly
            // paper over as "billed locally, close enough".
            log.error("Failed to post charge {} to Opera folio for reservation {} — left PENDING for retry.",
                    charge.getId(), charge.getOperaReservationId(), e);
        }
    }

    private void markPostedLocally(Charge charge) {
        charge.setStatus(ChargeStatus.POSTED_LOCALLY);
        charge.setPostedAt(java.time.LocalDateTime.now());
        chargeRepository.save(charge);
    }
}
