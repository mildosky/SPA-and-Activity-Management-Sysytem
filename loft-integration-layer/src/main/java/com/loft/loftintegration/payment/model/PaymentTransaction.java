package com.loft.loftintegration.payment.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * A payment transaction processed through a payment gateway.
 */
@Entity
@Table(name = "payment_transaction")
public class PaymentTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String propertyCode;

    /** The customer making the payment */
    @ManyToOne(optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    private com.loft.loftintegration.webshop.model.Customer customer;

    /** Optional: linked booking if this payment is for a booking */
    @ManyToOne(optional = true)
    @JoinColumn(name = "booking_id", nullable = true)
    private com.loft.loftintegration.booking.model.Booking booking;

    /** Optional: linked gift certificate if this payment is for purchasing a gift certificate */
    @ManyToOne(optional = true)
    @JoinColumn(name = "gift_certificate_id", nullable = true)
    private com.loft.loftintegration.webshop.model.GiftCertificate giftCertificate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentGatewayProvider provider;

    /** Gateway-specific transaction ID (e.g., Stripe charge ID) */
    @Column(nullable = false, unique = true)
    private String gatewayTransactionId;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus status = PaymentStatus.PENDING;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(nullable = true)
    private LocalDateTime completedAt;

    @Column(nullable = true, length = 4000)
    private String gatewayResponse;

    @Column(nullable = true, length = 4000)
    private String failureReason;

    protected PaymentTransaction() {
        // JPA
    }

    public PaymentTransaction(String propertyCode, 
                              com.loft.loftintegration.webshop.model.Customer customer,
                              PaymentGatewayProvider provider,
                              String gatewayTransactionId,
                              BigDecimal amount,
                              String currency) {
        this.propertyCode = propertyCode;
        this.customer = customer;
        this.provider = provider;
        this.gatewayTransactionId = gatewayTransactionId;
        this.amount = amount;
        this.currency = currency;
    }

    // Getters and setters

    public Long getId() { return id; }
    public String getPropertyCode() { return propertyCode; }
    public com.loft.loftintegration.webshop.model.Customer getCustomer() { return customer; }
    public com.loft.loftintegration.booking.model.Booking getBooking() { return booking; }
    public void setBooking(com.loft.loftintegration.booking.model.Booking booking) { this.booking = booking; }
    public com.loft.loftintegration.webshop.model.GiftCertificate getGiftCertificate() { return giftCertificate; }
    public void setGiftCertificate(com.loft.loftintegration.webshop.model.GiftCertificate giftCertificate) { this.giftCertificate = giftCertificate; }
    public PaymentGatewayProvider getProvider() { return provider; }
    public String getGatewayTransactionId() { return gatewayTransactionId; }
    public BigDecimal getAmount() { return amount; }
    public String getCurrency() { return currency; }
    public PaymentStatus getStatus() { return status; }
    public void setStatus(PaymentStatus status) { this.status = status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }
    public String getGatewayResponse() { return gatewayResponse; }
    public void setGatewayResponse(String gatewayResponse) { this.gatewayResponse = gatewayResponse; }
    public String getFailureReason() { return failureReason; }
    public void setFailureReason(String failureReason) { this.failureReason = failureReason; }
}
