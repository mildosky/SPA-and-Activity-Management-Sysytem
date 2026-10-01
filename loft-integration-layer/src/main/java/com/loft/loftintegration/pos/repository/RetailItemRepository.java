package com.loft.loftintegration.pos.repository;

import com.loft.loftintegration.pos.model.RetailItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RetailItemRepository extends JpaRepository<RetailItem, Long> {
    List<RetailItem> findByPropertyCodeAndActiveTrue(String propertyCode);
}
