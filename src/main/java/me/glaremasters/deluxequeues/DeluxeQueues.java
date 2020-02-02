package me.glaremasters.deluxequeues;

import co.aikar.commands.BungeeCommandManager;
import me.glaremasters.deluxequeues.acf.ACFHandler;
import me.glaremasters.deluxequeues.configuration.SettingsHandler;
import me.glaremasters.deluxequeues.listeners.ConnectionListener;
import me.glaremasters.deluxequeues.queues.QueueHandler;
import me.glaremasters.deluxequeues.updater.UpdateChecker;
import net.md_5.bungee.api.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;

public final class DeluxeQueues extends Plugin {

    private BungeeCommandManager commandManager;
    private SettingsHandler settingsHandler;
    private QueueHandler queueHandler;
    private ACFHandler acfHandler;

    @Override
    public void onEnable() {
        createFile("config.yml");
        createFile("languages/en-US.yml");
        settingsHandler = new SettingsHandler(this);
        UpdateChecker.runCheck(this, settingsHandler.getSettingsManager());
        startQueues();
        commandManager = new BungeeCommandManager(this);
        acfHandler = new ACFHandler(this, commandManager);
        getProxy().getPluginManager().registerListener(this, new ConnectionListener(this));
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }
    /**
     * Create a file to be used in the plugin
     * @param name the name of the file
     */
    private void createFile(String name) {
        if (!getDataFolder().exists()) {
            getDataFolder().mkdir();
        }
        File languageFolder = new File(getDataFolder(), "languages");
        if (!languageFolder.exists()) {
            languageFolder.mkdirs();
        }

        File file = new File(getDataFolder(), name);

        if (!file.exists()) {
            try (InputStream in = getResourceAsStream(name)) {
                Files.copy(in, file.toPath());
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }

    public BungeeCommandManager getCommandManager() {
        return this.commandManager;
    }

    public SettingsHandler getSettingsHandler() {
        return this.settingsHandler;
    }

    public QueueHandler getQueueHandler() {
        return this.queueHandler;
    }

    public void startQueues() {
        queueHandler = new QueueHandler(settingsHandler.getSettingsManager(), this);
        queueHandler.enableQueues();
    }
}
