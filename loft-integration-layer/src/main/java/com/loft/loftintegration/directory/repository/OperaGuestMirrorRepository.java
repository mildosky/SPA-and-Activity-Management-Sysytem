package com.loft.loftintegration.directory.repository;

import com.loft.loftintegration.directory.model.OperaGuestMirror;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

import java.util.List;

public interface OperaGuestMirrorRepository extends JpaRepository<OperaGuestMirror, Long> {
    Optional<OperaGuestMirror> findByOperaNameId(String operaNameId);

    /** Case-insensitive — email casing shouldn't be the reason a match fails. */
    Optional<OperaGuestMirror> findByEmailIgnoreCase(String email);

    List<OperaGuestMirror> findByPropertyCode(String propertyCode);

    /** Case-insensitive partial search on first or last name, for kiosk guest pickers. */
    List<OperaGuestMirror> findTop20ByFirstNameContainingIgnoreCaseOrLastNameContainingIgnoreCase(
            String firstName, String lastName);
}
