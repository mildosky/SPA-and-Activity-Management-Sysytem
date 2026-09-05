package com.loft.loftintegration;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Entry point for the Loft-style Opera PMS integration layer.
 *
 * This service is the first module of a larger spa/activity management
 * product. Its sole job at this stage is to normalize events coming out
 * of Opera PMS (reservations, guest profiles, folios) into a stable
 * internal model that later modules (booking, POS, staff scheduling)
 * can consume without knowing anything about Opera's quirks.
 *
 * @EnableScheduling turns on Spring's @Scheduled support — required for
 * OperaPollingScheduler's periodic poll to actually fire.
 */
@SpringBootApplication
@EnableScheduling
public class LoftIntegrationApplication {

    public static void main(String[] args) {
        SpringApplication.run(LoftIntegrationApplication.class, args);
    }
}
