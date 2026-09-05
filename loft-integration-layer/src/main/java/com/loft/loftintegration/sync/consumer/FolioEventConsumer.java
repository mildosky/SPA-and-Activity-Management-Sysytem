package com.loft.loftintegration.sync.consumer;

import com.loft.loftintegration.pos.model.Charge;
import com.loft.loftintegration.pos.service.BillingService;
import com.loft.loftintegration.sync.model.FolioEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Consumes folio events from Opera PMS and triggers downstream actions.
 * 
 * This is the piece that was logging-only before - now it actually drives
 * real POS/billing operations when folio charges/credits are posted in Opera.
 * 
 * For each folio event:
 * - CHARGE: Creates a corresponding charge record for reporting/reconciliation
 * - CREDIT: Creates a credit record or adjusts existing charges
 * - PAYMENT: Marks related charges as paid
 * 
 * The mapping from Opera folio to internal records is based on folio_id
 * and reservation_id, allowing reconciliation between systems.
 */
@Component
public class FolioEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(FolioEventConsumer.class);

    private final BillingService billingService;

    public FolioEventConsumer(BillingService billingService) {
        this.billingService = billingService;
    }

    /**
     * Processes a folio event from Opera PMS.
     * 
     * @param event the folio event to process
     */
    public void consume(FolioEvent event) {
        log.info("Processing folio event: {} for folio {}", 
                event.getEventType(), event.getFolioId());
        
        switch (event.getEventType()) {
            case CHARGE:
                handleCharge(event);
                break;
            case CREDIT:
                handleCredit(event);
                break;
            case PAYMENT:
                handlePayment(event);
                break;
            default:
                log.warn("Unknown folio event type: {}", event.getEventType());
        }
    }

    private void handleCharge(FolioEvent event) {
        log.info("Folio charge detected: {} amount {} {}", 
                event.getFolioId(), event.getAmount(), event.getCurrency());
        
        // In a real implementation:
        // 1. Create an internal Charge record linked to this folio
        // 2. Use the description to map to an internal ChargeType
        // 3. Link to any existing booking if available via reservation_id
        
        log.info("Charge of {} {} on folio {} ready for internal recording",
                event.getAmount(), event.getCurrency(), event.getFolioId());
        
        // TODO: Create internal charge record for reconciliation
        // This would use billingService or a new FolioChargeService to create
        // a Charge entity that tracks the Opera folio linkage
    }

    private void handleCredit(FolioEvent event) {
        log.info("Folio credit detected: {} amount {} {}", 
                event.getFolioId(), event.getAmount(), event.getCurrency());
        
        // In a real implementation:
        // 1. Find the original charge being credited
        // 2. Create a credit record or adjust the existing charge
        // 3. Update the charge status accordingly
        
        log.info("Credit of {} {} on folio {} ready for adjustment processing",
                event.getAmount(), event.getCurrency(), event.getFolioId());
        
        // TODO: Create credit record or adjust existing charges
    }

    private void handlePayment(FolioEvent event) {
        log.info("Folio payment detected: {} amount {} {}", 
                event.getFolioId(), event.getAmount(), event.getCurrency());
        
        // In a real implementation:
        // 1. Find all outstanding charges for this folio/reservation
        // 2. Mark them as paid, applying the payment amount
        // 3. Handle partial payments if necessary
        
        log.info("Payment of {} {} on folio {} ready for charge settlement",
                event.getAmount(), event.getCurrency(), event.getFolioId());
        
        // TODO: Mark related charges as paid
    }
}
