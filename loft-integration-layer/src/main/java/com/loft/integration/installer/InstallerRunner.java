package com.loft.integration.installer;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class InstallerRunner implements CommandLineRunner {

    private final InstallationWizard wizard;

    public InstallerRunner(InstallationWizard wizard) {
        this.wizard = wizard;
    }

    @Override
    public void run(String... args) throws Exception {
        // Check if already installed
        if (!wizard.isInstalled()) {
            System.out.println("\n>>> First-time installation detected. Launching Setup Wizard...\n");
            wizard.runWizard();
        } else {
            System.out.println("\n>>> Loft Integration Layer is already configured. Starting application...\n");
        }
    }
}
