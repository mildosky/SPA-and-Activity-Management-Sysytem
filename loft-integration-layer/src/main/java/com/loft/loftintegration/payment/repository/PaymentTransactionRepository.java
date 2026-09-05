package com.loft.loftintegration.payment.repository;

import com.loft.loftintegration.payment.model.PaymentTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PaymentTransactionRepository extends JpaRepository<PaymentTransaction, Long> {
    
    Optional<PaymentTransaction> findByGatewayTransactionId(String gatewayTransactionId);
    
    Optional<PaymentTransaction> findByBookingId(Long bookingId);
}
