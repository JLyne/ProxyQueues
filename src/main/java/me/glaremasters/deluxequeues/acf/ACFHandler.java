package me.glaremasters.deluxequeues.acf;

import co.aikar.commands.VelocityCommandManager;
import co.aikar.locales.MessageKey;
import me.glaremasters.deluxequeues.DeluxeQueues;
import me.glaremasters.deluxequeues.commands.CommandHelp;
import me.glaremasters.deluxequeues.commands.CommandLeave;
import me.glaremasters.deluxequeues.queues.QueueHandler;
import ninja.leaping.configurate.ConfigurationNode;
import ninja.leaping.configurate.yaml.YAMLConfigurationLoader;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public class ACFHandler {

    private DeluxeQueues deluxeQueues;

    public ACFHandler(DeluxeQueues deluxeQueues, VelocityCommandManager commandManager) {
        this.deluxeQueues = deluxeQueues;
        commandManager.enableUnstableAPI("help");
        registerLanguages(deluxeQueues, commandManager);
        registerDependencyInjection(commandManager);
        registerCommandReplacements(commandManager);

        registerCommands(commandManager);
    }

    public void registerDependencyInjection(VelocityCommandManager commandManager) {
        commandManager.registerDependency(QueueHandler.class, deluxeQueues.getQueueHandler());
    }

    public void registerCommandReplacements(VelocityCommandManager commandManager) {
        commandManager.getCommandReplacements().addReplacement("dq", "queue|dq|queues");
    }

    public void registerCommands(VelocityCommandManager commandManager) {
        commandManager.registerCommand(new CommandHelp());
        commandManager.registerCommand(new CommandLeave());
    }

    /**
     * Load all the language files for the plugin
     * @param commandManager command manager
     */
    public void registerLanguages(DeluxeQueues deluxeQueues, VelocityCommandManager commandManager) {
        try {
            File languageFolder = new File(deluxeQueues.getDataFolder(), "languages");
            for (File file : Objects.requireNonNull(languageFolder.listFiles())) {
                if (file.isFile()) {
                    if (file.getName().endsWith(".yml")) {
                        String updatedName = file.getName().replace(".yml", "");
                        commandManager.addSupportedLanguage(Locale.forLanguageTag(updatedName));

                        ConfigurationNode config = YAMLConfigurationLoader.builder().setFile(
					new File(languageFolder, file.getName())).build().load();

                        loadLanguage(config, Locale.forLanguageTag(updatedName), commandManager);
                    }
                }
            }
            commandManager.getLocales().setDefaultLocale(Locale.forLanguageTag("en-US"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Loads every message from the Configuration object. Any nested values will be treated as namespace
     * so acf-core:\n\tfoo: bar will be acf-core.foo = bar
     *
     * @param config
     * @param locale
     */
    public void loadLanguage(ConfigurationNode config, Locale locale, VelocityCommandManager commandManager) {
        boolean loaded = false;
        for (ConfigurationNode parent : config.getChildrenList()) {
            List<? extends ConfigurationNode> inner = parent.getChildrenList();

            if (inner.isEmpty()) {
                continue;
            }

            for (ConfigurationNode key : inner) {
                String value = key.getString();

                if (value != null && !value.isEmpty()) {
                    commandManager.getLocales().addMessage(locale, MessageKey.of(key.getKey().toString() + "." + key), value);
                    loaded = true;
                }
            }
        }
    }
}
