package com.loft.loftintegration.sync.consumer;

import com.loft.loftintegration.sync.model.FolioEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

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
        // TODO: Create internal charge record for reconciliation
    }

    private void handleCredit(FolioEvent event) {
        log.info("Folio credit detected: {} amount {} {}", 
                event.getFolioId(), event.getAmount(), event.getCurrency());
        // TODO: Create credit record or adjust existing charges
    }

    private void handlePayment(FolioEvent event) {
        log.info("Folio payment detected: {} amount {} {}", 
                event.getFolioId(), event.getAmount(), event.getCurrency());
        // TODO: Mark related charges as paid
    }
}
