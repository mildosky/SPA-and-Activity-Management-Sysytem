package com.loft.loftintegration.sync.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Normalized folio/billing event, PMS-agnostic.
 *
 * Exists so a future POS module (charging a spa treatment to a guest's
 * room folio) has a stable target to post charges against, regardless
 * of whether the underlying PMS is Opera v5 or something else later.
 */
public class FolioEvent {

    public enum EventType { CHARGE_POSTED, CHARGE_VOIDED, FOLIO_CLOSED }

    private String folioId;
    private String reservationId;
    private String propertyCode;
    private BigDecimal amount;
    private String currency;
    private String description;
    private EventType eventType;
    private LocalDateTime eventTimestamp;

    public FolioEvent() {
    }

    public FolioEvent(String folioId, String reservationId, String propertyCode, BigDecimal amount,
                       String currency, String description, EventType eventType, LocalDateTime eventTimestamp) {
        this.folioId = folioId;
        this.reservationId = reservationId;
        this.propertyCode = propertyCode;
        this.amount = amount;
        this.currency = currency;
        this.description = description;
        this.eventType = eventType;
        this.eventTimestamp = eventTimestamp;
    }

    public String getFolioId() { return folioId; }
    public void setFolioId(String folioId) { this.folioId = folioId; }

    public String getReservationId() { return reservationId; }
    public void setReservationId(String reservationId) { this.reservationId = reservationId; }

    public String getPropertyCode() { return propertyCode; }
    public void setPropertyCode(String propertyCode) { this.propertyCode = propertyCode; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public EventType getEventType() { return eventType; }
    public void setEventType(EventType eventType) { this.eventType = eventType; }

    public LocalDateTime getEventTimestamp() { return eventTimestamp; }
    public void setEventTimestamp(LocalDateTime eventTimestamp) { this.eventTimestamp = eventTimestamp; }
}
