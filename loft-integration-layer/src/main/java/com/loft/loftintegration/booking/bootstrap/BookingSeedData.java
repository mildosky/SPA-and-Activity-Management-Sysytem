package com.loft.loftintegration.booking.bootstrap;

import com.loft.loftintegration.booking.model.*;
import com.loft.loftintegration.booking.repository.ActivityTypeRepository;
import com.loft.loftintegration.booking.repository.ResourceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Seeds example ActivityTypes and Resources on first startup, so the
 * booking flow can be exercised via BookingController immediately
 * without hand-writing SQL first. Demonstrates the design point: a
 * treatment (ROOM + STAFF), tennis (COURT), and a golf tee time
 * (TEE_TIME_SLOT) are all created through the exact same ActivityType/
 * ResourceRequirement shape — no special-casing per category.
 *
 * Only seeds if the ActivityType table is empty, so this is safe to
 * leave running — it won't duplicate data on every restart. Remove or
 * disable once real property-specific catalog data exists.
 *
 * @Order(1): must run before StaffScheduleSeedData, which needs the
 * STAFF resources created here to already exist.
 */
@Component
@Order(1)
public class BookingSeedData implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(BookingSeedData.class);
    private static final String PROPERTY_CODE = "TGL";

    private final ActivityTypeRepository activityTypeRepository;
    private final ResourceRepository resourceRepository;

    public BookingSeedData(ActivityTypeRepository activityTypeRepository, ResourceRepository resourceRepository) {
        this.activityTypeRepository = activityTypeRepository;
        this.resourceRepository = resourceRepository;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!activityTypeRepository.findByPropertyCode(PROPERTY_CODE).isEmpty()) {
            log.debug("Booking seed data already present for {}, skipping.", PROPERTY_CODE);
            return;
        }

        // Resources
        resourceRepository.save(new Resource(PROPERTY_CODE, ResourceType.ROOM, "Treatment Room 1"));
        resourceRepository.save(new Resource(PROPERTY_CODE, ResourceType.ROOM, "Treatment Room 2"));
        resourceRepository.save(new Resource(PROPERTY_CODE, ResourceType.STAFF, "Sarah (Massage Therapist)"));
        resourceRepository.save(new Resource(PROPERTY_CODE, ResourceType.STAFF, "James (Massage Therapist)"));
        resourceRepository.save(new Resource(PROPERTY_CODE, ResourceType.COURT, "Tennis Court 1"));
        resourceRepository.save(new Resource(PROPERTY_CODE, ResourceType.TEE_TIME_SLOT, "Hole 1 - 8:00am"));
        resourceRepository.save(new Resource(PROPERTY_CODE, ResourceType.TEE_TIME_SLOT, "Hole 1 - 8:10am"));
        resourceRepository.save(new Resource(PROPERTY_CODE, ResourceType.LOUNGER, "Poolside Lounger 1"));
        resourceRepository.save(new Resource(PROPERTY_CODE, ResourceType.LOUNGER, "Poolside Lounger 2"));
        resourceRepository.save(new Resource(PROPERTY_CODE, ResourceType.LOUNGER, "Poolside Lounger 3"));

        // Treatment: needs 1 ROOM + 1 STAFF
        ActivityType massage = new ActivityType(PROPERTY_CODE, "60-Minute Deep Tissue Massage",
                ActivityCategory.TREATMENT, 60, new java.math.BigDecimal("45000"), "NGN");
        massage.addRequirement(new ResourceRequirement("ROOM", ResourceType.ROOM, 1));
        massage.addRequirement(new ResourceRequirement("THERAPIST", ResourceType.STAFF, 1));
        activityTypeRepository.save(massage);

        // Racquet sport: needs 1 COURT
        ActivityType tennis = new ActivityType(PROPERTY_CODE, "Tennis Court Hire",
                ActivityCategory.RACQUET_SPORT, 60, new java.math.BigDecimal("15000"), "NGN");
        tennis.addRequirement(new ResourceRequirement("COURT", ResourceType.COURT, 1));
        activityTypeRepository.save(tennis);

        // Golf: needs 1 TEE_TIME_SLOT — same shape as everything above
        ActivityType golf = new ActivityType(PROPERTY_CODE, "18-Hole Tee Time",
                ActivityCategory.GOLF, 240, new java.math.BigDecimal("60000"), "NGN");
        golf.addRequirement(new ResourceRequirement("TEE_TIME", ResourceType.TEE_TIME_SLOT, 1));
        activityTypeRepository.save(golf);

        // Poolside lounger reservation: needs 1 LOUNGER — same shape again.
        // This is the point: loungers required zero new booking-core code,
        // just this ActivityType + the LOUNGER resources above.
        ActivityType lounger = new ActivityType(PROPERTY_CODE, "Poolside Lounger Reservation",
                ActivityCategory.GENERIC_ACTIVITY, 240, new java.math.BigDecimal("5000"), "NGN");
        lounger.addRequirement(new ResourceRequirement("LOUNGER", ResourceType.LOUNGER, 1));
        activityTypeRepository.save(lounger);

        log.info("Seeded booking data for {}: {} resources, 4 activity types (massage, tennis, golf, lounger).",
                PROPERTY_CODE, resourceRepository.findByPropertyCodeAndResourceTypeAndActiveTrue(PROPERTY_CODE, ResourceType.ROOM).size()
                        + resourceRepository.findByPropertyCodeAndResourceTypeAndActiveTrue(PROPERTY_CODE, ResourceType.STAFF).size()
                        + resourceRepository.findByPropertyCodeAndResourceTypeAndActiveTrue(PROPERTY_CODE, ResourceType.COURT).size()
                        + resourceRepository.findByPropertyCodeAndResourceTypeAndActiveTrue(PROPERTY_CODE, ResourceType.TEE_TIME_SLOT).size()
                        + resourceRepository.findByPropertyCodeAndResourceTypeAndActiveTrue(PROPERTY_CODE, ResourceType.LOUNGER).size());
    }
}
