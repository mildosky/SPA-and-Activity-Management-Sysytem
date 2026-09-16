package com.loft.loftintegration.booking.bootstrap;

import com.loft.loftintegration.booking.model.Resource;
import com.loft.loftintegration.booking.model.ResourceType;
import com.loft.loftintegration.booking.model.ShiftTemplate;
import com.loft.loftintegration.booking.repository.ResourceRepository;
import com.loft.loftintegration.booking.repository.ShiftTemplateRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * Seeds shift templates for the STAFF resources BookingSeedData
 * already created (Sarah, James). Gives them genuinely different
 * schedules on purpose — Sarah Mon-Fri daytime, James Tue-Sat
 * afternoon/evening — so testing can actually distinguish "on shift"
 * from "off shift" rather than both staff always being available.
 *
 * IMPORTANT — this is a real behavior change, not just an addition:
 * AvailabilityService now treats a STAFF resource with NO shift
 * templates as permanently unavailable (conservative default). Any
 * staff resource that existed before this seed data was added needs
 * a real shift template to remain bookable at all.
 *
 * Own independent guard (count on THIS table, not BookingSeedData's
 * ActivityType check) — deliberately, so this seeds correctly even
 * against a database where BookingSeedData already ran and its own
 * guard would otherwise skip everything new. Hit exactly this problem
 * once already with the lounger addition — not repeating it.
 *
 * @Order(2): must run after BookingSeedData (@Order(1)), which
 * creates the staff Resources this depends on.
 */
@Component
@Order(2)
public class StaffScheduleSeedData implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(StaffScheduleSeedData.class);
    private static final String PROPERTY_CODE = "TGL";

    private final ResourceRepository resourceRepository;
    private final ShiftTemplateRepository shiftTemplateRepository;

    public StaffScheduleSeedData(ResourceRepository resourceRepository,
                                  ShiftTemplateRepository shiftTemplateRepository) {
        this.resourceRepository = resourceRepository;
        this.shiftTemplateRepository = shiftTemplateRepository;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (shiftTemplateRepository.countByResourcePropertyCode(PROPERTY_CODE) > 0) {
            log.debug("Shift template seed data already present for {}, skipping.", PROPERTY_CODE);
            return;
        }

        List<Resource> staff = resourceRepository
                .findByPropertyCodeAndResourceTypeAndActiveTrue(PROPERTY_CODE, ResourceType.STAFF);

        if (staff.isEmpty()) {
            log.warn("No STAFF resources found for {} — shift templates cannot be seeded until "
                    + "BookingSeedData has run and created staff resources.", PROPERTY_CODE);
            return;
        }

        int seeded = 0;
        for (Resource staffMember : staff) {
            if (staffMember.getName().contains("Sarah")) {
                shiftTemplateRepository.save(new ShiftTemplate(
                        staffMember, "Weekday Daytime",
                        EnumSet.of(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
                                DayOfWeek.THURSDAY, DayOfWeek.FRIDAY),
                        LocalTime.of(9, 0), LocalTime.of(17, 0)));
                seeded++;
            } else if (staffMember.getName().contains("James")) {
                shiftTemplateRepository.save(new ShiftTemplate(
                        staffMember, "Afternoon/Evening",
                        EnumSet.of(DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY,
                                DayOfWeek.FRIDAY, DayOfWeek.SATURDAY),
                        LocalTime.of(12, 0), LocalTime.of(20, 0)));
                seeded++;
            } else {
                // Any other staff resource added later without a
                // matching name pattern here gets no shift template —
                // and will be permanently unavailable until one is
                // added. Logged loudly rather than silently.
                log.warn("STAFF resource '{}' has no matching shift template rule in "
                        + "StaffScheduleSeedData — it will be unavailable for booking until "
                        + "a shift template is added for it.", staffMember.getName());
            }
        }

        log.info("Seeded {} shift template(s) for {}.", seeded, PROPERTY_CODE);
    }
}
