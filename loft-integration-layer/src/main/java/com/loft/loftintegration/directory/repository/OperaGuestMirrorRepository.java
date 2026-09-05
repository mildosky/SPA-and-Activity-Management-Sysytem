package com.loft.loftintegration.directory.repository;

import com.loft.loftintegration.directory.model.OperaGuestMirror;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OperaGuestMirrorRepository extends JpaRepository<OperaGuestMirror, Long> {
    Optional<OperaGuestMirror> findByOperaNameId(String operaNameId);

    /** Case-insensitive — email casing shouldn't be the reason a match fails. */
    Optional<OperaGuestMirror> findByEmailIgnoreCase(String email);
}
