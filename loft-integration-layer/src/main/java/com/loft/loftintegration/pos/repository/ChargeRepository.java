package com.loft.loftintegration.pos.repository;

import com.loft.loftintegration.pos.model.Charge;
import com.loft.loftintegration.pos.model.ChargeStatus;
import com.loft.loftintegration.pos.model.ChargeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;

public interface ChargeRepository extends JpaRepository<Charge, Long> {
    List<Charge> findByChargeTypeAndSourceId(ChargeType chargeType, Long sourceId);

    /** COALESCE guards against SUM returning null when there are zero matching rows — callers get 0, not null. */
    @Query("SELECT COALESCE(SUM(c.amount), 0) FROM Charge c WHERE c.propertyCode = :propertyCode AND c.status = :status")
    BigDecimal sumAmountByPropertyCodeAndStatus(@Param("propertyCode") String propertyCode, @Param("status") ChargeStatus status);

    @Query("SELECT COALESCE(SUM(c.amount), 0) FROM Charge c WHERE c.propertyCode = :propertyCode "
            + "AND c.chargeType = :chargeType AND c.status <> com.loft.loftintegration.pos.model.ChargeStatus.VOIDED")
    BigDecimal sumAmountByPropertyCodeAndChargeType(@Param("propertyCode") String propertyCode, @Param("chargeType") ChargeType chargeType);
}
