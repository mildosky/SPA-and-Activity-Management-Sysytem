package com.loft.loftintegration.booking.repository;

import com.loft.loftintegration.booking.model.Resource;
import com.loft.loftintegration.booking.model.ResourceType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ResourceRepository extends JpaRepository<Resource, Long> {
    List<Resource> findByPropertyCodeAndResourceTypeAndActiveTrue(String propertyCode, ResourceType resourceType);
}
