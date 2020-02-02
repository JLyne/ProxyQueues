package me.glaremasters.deluxequeues.acf;

import ch.jalu.configme.SettingsManager;
import co.aikar.commands.BungeeCommandManager;
import me.glaremasters.deluxequeues.DeluxeQueues;
import me.glaremasters.deluxequeues.commands.CommandHelp;
import me.glaremasters.deluxequeues.commands.CommandLeave;
import me.glaremasters.deluxequeues.commands.CommandReload;
import me.glaremasters.deluxequeues.queues.QueueHandler;

import java.io.File;
import java.io.IOException;
import java.util.Locale;
import java.util.Objects;

public class ACFHandler {

    private DeluxeQueues deluxeQueues;

    public ACFHandler(DeluxeQueues deluxeQueues, BungeeCommandManager commandManager) {
        this.deluxeQueues = deluxeQueues;
        commandManager.enableUnstableAPI("help");
        registerLanguages(deluxeQueues, commandManager);
        registerDependencyInjection(commandManager);
        registerCommandReplacements(commandManager);

        registerCommands(commandManager);
    }

    public void registerDependencyInjection(BungeeCommandManager commandManager) {
        commandManager.registerDependency(QueueHandler.class, deluxeQueues.getQueueHandler());
        commandManager.registerDependency(SettingsManager.class, deluxeQueues.getSettingsHandler().getSettingsManager());
    }

    public void registerCommandReplacements(BungeeCommandManager commandManager) {
        commandManager.getCommandReplacements().addReplacement("dq", "queue|dq|queues");
    }

    public void registerCommands(BungeeCommandManager commandManager) {
        commandManager.registerCommand(new CommandHelp());
        commandManager.registerCommand(new CommandLeave());
        commandManager.registerCommand(new CommandReload());
    }

    /**
     * Load all the language files for the plugin
     * @param commandManager command manager
     */
    public void registerLanguages(DeluxeQueues deluxeQueues, BungeeCommandManager commandManager) {
        try {
            File languageFolder = new File(deluxeQueues.getDataFolder(), "languages");
            for (File file : Objects.requireNonNull(languageFolder.listFiles())) {
                if (file.isFile()) {
                    if (file.getName().endsWith(".yml")) {
                        String updatedName = file.getName().replace(".yml", "");
                        commandManager.addSupportedLanguage(Locale.forLanguageTag(updatedName));
                        commandManager.getLocales().loadYamlLanguageFile(new File(languageFolder, file.getName()), Locale.forLanguageTag(updatedName));
                    }
                }
            }
            commandManager.getLocales().setDefaultLocale(Locale.forLanguageTag("en-US"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


}
