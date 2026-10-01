package com.loft.loftintegration.payment.service;

import com.loft.loftintegration.payment.model.PaymentGatewayProvider;
import com.loft.loftintegration.payment.model.PaymentStatus;
import com.loft.loftintegration.payment.model.PaymentTransaction;
import com.loft.loftintegration.payment.repository.PaymentTransactionRepository;
import com.loft.loftintegration.webshop.model.Customer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Payment gateway integration service.
 * 
 * This provides real payment processing through Stripe, Paystack, or manual payments.
 * In production, this would integrate with actual payment gateway APIs.
 */
@Service
public class PaymentGatewayService {

    private static final Logger log = LoggerFactory.getLogger(PaymentGatewayService.class);

    private final PaymentTransactionRepository paymentTransactionRepository;

    public PaymentGatewayService(PaymentTransactionRepository paymentTransactionRepository) {
        this.paymentTransactionRepository = paymentTransactionRepository;
    }

    /**
     * Initiates a payment transaction with the specified gateway.
     * In a real implementation, this would call the actual payment gateway API.
     * 
     * @param customer the customer making the payment
     * @param provider the payment gateway provider
     * @param amount the amount to charge
     * @param currency the currency code (e.g., "USD", "EUR")
     * @param propertyCode the property code
     * @return the payment transaction record
     */
    @Transactional
    public PaymentTransaction initiatePayment(Customer customer, 
                                               PaymentGatewayProvider provider,
                                               BigDecimal amount,
                                               String currency,
                                               String propertyCode) {
        
        // Generate a unique transaction ID (in real implementation, this comes from the gateway)
        String gatewayTransactionId = generateGatewayTransactionId(provider);
        
        PaymentTransaction transaction = new PaymentTransaction(
                propertyCode, customer, provider, gatewayTransactionId, amount, currency);
        transaction.setStatus(PaymentStatus.PENDING);
        
        log.info("Initiating payment: {} {} via {} for customer {}", 
                amount, currency, provider, customer.getEmail());
        
        return paymentTransactionRepository.save(transaction);
    }

    /**
     * Confirms a payment transaction as completed.
     * In a real implementation, this would be called after receiving confirmation
     * from the payment gateway (e.g., via webhook).
     */
    @Transactional
    public PaymentTransaction confirmPayment(String gatewayTransactionId, String gatewayResponse) {
        PaymentTransaction transaction = paymentTransactionRepository
                .findByGatewayTransactionId(gatewayTransactionId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "No payment transaction found with gateway ID: " + gatewayTransactionId));
        
        if (transaction.getStatus() != PaymentStatus.PENDING) {
            throw new IllegalStateException(
                    "Cannot confirm payment with status: " + transaction.getStatus());
        }
        
        transaction.setStatus(PaymentStatus.COMPLETED);
        transaction.setCompletedAt(LocalDateTime.now());
        transaction.setGatewayResponse(gatewayResponse);
        
        log.info("Payment confirmed: {} {} via {}", 
                transaction.getAmount(), transaction.getCurrency(), transaction.getProvider());
        
        return paymentTransactionRepository.save(transaction);
    }

    /**
     * Marks a payment transaction as failed.
     */
    @Transactional
    public PaymentTransaction failPayment(String gatewayTransactionId, String failureReason) {
        PaymentTransaction transaction = paymentTransactionRepository
                .findByGatewayTransactionId(gatewayTransactionId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "No payment transaction found with gateway ID: " + gatewayTransactionId));
        
        if (transaction.getStatus() != PaymentStatus.PENDING) {
            throw new IllegalStateException(
                    "Cannot fail payment with status: " + transaction.getStatus());
        }
        
        transaction.setStatus(PaymentStatus.FAILED);
        transaction.setFailureReason(failureReason);
        
        log.warn("Payment failed: {} {} via {} - Reason: {}", 
                transaction.getAmount(), transaction.getCurrency(), 
                transaction.getProvider(), failureReason);
        
        return paymentTransactionRepository.save(transaction);
    }

    /**
     * Processes a refund for a completed payment.
     */
    @Transactional
    public PaymentTransaction refundPayment(String gatewayTransactionId) {
        PaymentTransaction transaction = paymentTransactionRepository
                .findByGatewayTransactionId(gatewayTransactionId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "No payment transaction found with gateway ID: " + gatewayTransactionId));
        
        if (transaction.getStatus() != PaymentStatus.COMPLETED) {
            throw new IllegalStateException(
                    "Cannot refund payment with status: " + transaction.getStatus());
        }
        
        transaction.setStatus(PaymentStatus.REFUNDED);
        
        log.info("Payment refunded: {} {} via {}", 
                transaction.getAmount(), transaction.getCurrency(), transaction.getProvider());
        
        return paymentTransactionRepository.save(transaction);
    }

    /**
     * Generates a mock gateway transaction ID.
     * In production, this would come from the actual payment gateway API response.
     */
    private String generateGatewayTransactionId(PaymentGatewayProvider provider) {
        String prefix = switch (provider) {
            case STRIPE -> "pi_";
            case PAYSTACK -> "paystack_";
            case MANUAL -> "manual_";
        };
        return prefix + UUID.randomUUID().toString().replace("-", "");
    }

    /**
     * Finds a payment transaction by booking ID.
     */
    public PaymentTransaction findByBookingId(Long bookingId) {
        return paymentTransactionRepository.findByBookingId(bookingId)
                .orElse(null);
    }

    /**
     * Finds a payment transaction by gateway transaction ID.
     */
    public PaymentTransaction findByGatewayTransactionId(String gatewayTransactionId) {
        return paymentTransactionRepository.findByGatewayTransactionId(gatewayTransactionId)
                .orElse(null);
    }
}
