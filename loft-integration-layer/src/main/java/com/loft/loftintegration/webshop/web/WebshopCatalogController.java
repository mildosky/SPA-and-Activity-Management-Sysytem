package com.loft.loftintegration.webshop.web;

import com.loft.loftintegration.booking.repository.ActivityTypeRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Lets a guest browse what's bookable before drilling into availability
 * for a specific one — every other webshop endpoint requires already
 * knowing an activityTypeId, which a real guest-facing UI can't assume.
 */
@RestController
@RequestMapping("/api/webshop")
public class WebshopCatalogController {

    private final ActivityTypeRepository activityTypeRepository;

    public WebshopCatalogController(ActivityTypeRepository activityTypeRepository) {
        this.activityTypeRepository = activityTypeRepository;
    }

    @GetMapping("/activity-types")
    public List<ActivityTypeResponse> listActivityTypes(@RequestParam("propertyCode") String propertyCode) {
        return activityTypeRepository.findByPropertyCode(propertyCode).stream()
                .map(ActivityTypeResponse::from)
                .toList();
    }
}
