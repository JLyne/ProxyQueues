package uk.co.notnull.proxyqueues.configuration;

import ch.jalu.configme.configurationdata.ConfigurationData;
import ch.jalu.configme.configurationdata.ConfigurationDataBuilder;
import uk.co.notnull.proxyqueues.configuration.sections.ConfigOptions;

/**
 * Created by Glare
 * Date: 7/13/2019
 * Time: 10:27 PM
 */
public class ConfigBuilder {

    private ConfigBuilder() {

    }

    public static ConfigurationData buildConfig() {
        return ConfigurationDataBuilder
                .createConfiguration(ConfigOptions.class);
    }

}
