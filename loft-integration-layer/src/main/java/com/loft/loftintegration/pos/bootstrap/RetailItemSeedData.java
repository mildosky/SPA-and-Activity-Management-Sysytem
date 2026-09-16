package com.loft.loftintegration.pos.bootstrap;

import com.loft.loftintegration.pos.model.RetailItem;
import com.loft.loftintegration.pos.repository.RetailItemRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Seeds example retail items for property TGL. Own independent guard
 * (checks THIS table, not BookingSeedData's) — same lesson applied as
 * StaffScheduleSeedData: seeds correctly on an existing database
 * without needing a wipe, regardless of what other seed data has
 * already run.
 */
@Component
public class RetailItemSeedData implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(RetailItemSeedData.class);
    private static final String PROPERTY_CODE = "TGL";

    private final RetailItemRepository retailItemRepository;

    public RetailItemSeedData(RetailItemRepository retailItemRepository) {
        this.retailItemRepository = retailItemRepository;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!retailItemRepository.findByPropertyCodeAndActiveTrue(PROPERTY_CODE).isEmpty()) {
            log.debug("Retail item seed data already present for {}, skipping.", PROPERTY_CODE);
            return;
        }

        retailItemRepository.save(new RetailItem(PROPERTY_CODE, "Sunscreen SPF50", new BigDecimal("3500"), "NGN"));
        retailItemRepository.save(new RetailItem(PROPERTY_CODE, "Branded Beach Towel", new BigDecimal("8000"), "NGN"));
        retailItemRepository.save(new RetailItem(PROPERTY_CODE, "Bottled Water", new BigDecimal("1000"), "NGN"));

        log.info("Seeded 3 retail item(s) for {}.", PROPERTY_CODE);
    }
}
