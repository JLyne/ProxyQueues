package me.glaremasters.deluxequeues.configuration;

import ch.jalu.configme.SettingsManager;
import ch.jalu.configme.SettingsManagerBuilder;
import ch.jalu.configme.migration.PlainMigrationService;
import me.glaremasters.deluxequeues.DeluxeQueues;

import java.io.File;

/**
 * Created by Glare
 * Date: 7/13/2019
 * Time: 10:24 PM
 */
public class SettingsHandler {

    private DeluxeQueues deluxeQueues;
    private SettingsManager settingsManager;

    public SettingsHandler(DeluxeQueues deluxeQueues) {
        this.deluxeQueues = deluxeQueues;

        settingsManager = SettingsManagerBuilder
                .withYamlFile(new File(deluxeQueues.getDataFolder(), "config.yml"))
                .migrationService(new PlainMigrationService())
                .configurationData(ConfigBuilder.buildConfig())
                .create();
    }

    public SettingsManager getSettingsManager() {
        return settingsManager;
    }
}
