package com.loft.loftintegration.directory.repository;

import com.loft.loftintegration.directory.model.InHouseReservationMirror;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface InHouseReservationMirrorRepository extends JpaRepository<InHouseReservationMirror, Long> {
    Optional<InHouseReservationMirror> findByOperaReservationId(String operaReservationId);
    Optional<InHouseReservationMirror> findByGuestProfileId(String guestProfileId);
    List<InHouseReservationMirror> findByPropertyCode(String propertyCode);
}
