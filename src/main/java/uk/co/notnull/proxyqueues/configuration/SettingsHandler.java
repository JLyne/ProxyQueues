package uk.co.notnull.proxyqueues.configuration;

import ch.jalu.configme.SettingsManager;
import ch.jalu.configme.SettingsManagerBuilder;
import ch.jalu.configme.migration.PlainMigrationService;
import uk.co.notnull.proxyqueues.ProxyQueues;

import java.io.File;

/**
 * Created by Glare
 * Date: 7/13/2019
 * Time: 10:24 PM
 */
public class SettingsHandler {

    private final SettingsManager settingsManager;

    public SettingsHandler(ProxyQueues proxyQueues) {

        settingsManager = SettingsManagerBuilder
                .withYamlFile(new File(proxyQueues.getDataFolder(), "config.yml"))
                .migrationService(new PlainMigrationService())
                .configurationData(ConfigBuilder.buildConfig())
                .create();
    }

    public SettingsManager getSettingsManager() {
        return settingsManager;
    }
}
