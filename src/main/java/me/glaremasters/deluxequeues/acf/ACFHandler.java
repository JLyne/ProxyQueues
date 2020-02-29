package me.glaremasters.deluxequeues.acf;

import co.aikar.commands.MessageType;
import co.aikar.commands.VelocityCommandManager;
import co.aikar.locales.MessageKey;
import ch.jalu.configme.SettingsManager;
import me.glaremasters.deluxequeues.DeluxeQueues;
import me.glaremasters.deluxequeues.commands.*;
import me.glaremasters.deluxequeues.queues.QueueHandler;
import net.kyori.text.format.TextColor;
import ninja.leaping.configurate.ConfigurationNode;
import ninja.leaping.configurate.yaml.YAMLConfigurationLoader;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class ACFHandler {

    private final DeluxeQueues deluxeQueues;

    public ACFHandler(DeluxeQueues deluxeQueues, VelocityCommandManager commandManager) {
        this.deluxeQueues = deluxeQueues;
        //noinspection deprecation
        commandManager.enableUnstableAPI("help");
        registerLanguages(deluxeQueues, commandManager);
        registerDependencyInjection(commandManager);
        registerCommandReplacements(commandManager);

        commandManager.setFormat(MessageType.ERROR, TextColor.GOLD, TextColor.RED, TextColor.YELLOW);
        commandManager.setFormat(MessageType.INFO, TextColor.BLUE, TextColor.GREEN, TextColor.LIGHT_PURPLE);

        registerCommands(commandManager);
    }

    public void registerDependencyInjection(VelocityCommandManager commandManager) {
        commandManager.registerDependency(QueueHandler.class, deluxeQueues.getQueueHandler());
        commandManager.registerDependency(SettingsManager.class, deluxeQueues.getSettingsHandler().getSettingsManager());
    }

    public void registerCommandReplacements(VelocityCommandManager commandManager) {
        commandManager.getCommandReplacements().addReplacement("dq", "queue|dq");
    }

    public void registerCommands(VelocityCommandManager commandManager) {
        commandManager.registerCommand(new CommandHelp());
        commandManager.registerCommand(new CommandJoin());
        commandManager.registerCommand(new CommandLeave());
        commandManager.registerCommand(new CommandReload());
    }

    /**
     * Load all the language files for the plugin
     * @param commandManager command manager
     */
    public void registerLanguages(DeluxeQueues deluxeQueues, VelocityCommandManager commandManager) {
        try {
            File languageFolder = new File(deluxeQueues.getDataFolder().getAbsolutePath(), "languages");

            for (File file : Objects.requireNonNull(languageFolder.listFiles())) {
                if (file.isFile()) {
                    if (file.getName().endsWith(".yml")) {
                        String updatedName = file.getName().replace(".yml", "");
                        commandManager.addSupportedLanguage(Locale.forLanguageTag(updatedName));

                        ConfigurationNode config = YAMLConfigurationLoader.builder().setFile(file).build().load();
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
        config.getChildrenMap().forEach((key, node) -> {;
            Map<Object, ? extends ConfigurationNode> inner = node.getChildrenMap();

            if (inner.isEmpty()) {
                return;
            }

            inner.forEach((innerKey, innerNode) -> {
                String value = innerNode.getString();

                String s = key.toString() + "." + innerKey.toString();

                if (value != null && !value.isEmpty()) {
                    commandManager.getLocales().addMessage(locale, MessageKey.of(
                            s), value);
                }
            });
        });
    }
}
