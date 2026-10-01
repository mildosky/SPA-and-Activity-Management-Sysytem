package com.loft.loftintegration.webshop.repository;

import com.loft.loftintegration.webshop.model.GiftCertificate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface GiftCertificateRepository extends JpaRepository<GiftCertificate, Long> {
    Optional<GiftCertificate> findByCode(String code);
}
