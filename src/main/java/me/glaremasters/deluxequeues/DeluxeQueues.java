package me.glaremasters.deluxequeues;

import co.aikar.commands.BungeeCommandManager;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import me.glaremasters.deluxequeues.acf.ACFHandler;
import me.glaremasters.deluxequeues.configuration.SettingsHandler;
import me.glaremasters.deluxequeues.listeners.ConnectionListener;
import me.glaremasters.deluxequeues.queues.QueueHandler;
import me.glaremasters.deluxequeues.updater.UpdateChecker;
import org.slf4j.Logger;

import javax.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

@Plugin(id="deluxequeues", name="DeluxeQueues")
public final class DeluxeQueues {

    private BungeeCommandManager commandManager;
    private SettingsHandler settingsHandler;
    private QueueHandler queueHandler;
    private ACFHandler acfHandler;

    private final ProxyServer proxyServer;
    private final Logger logger;

    @Inject
    @DataDirectory
    private Path dataFolder;

    @Inject
    public DeluxeQueues(ProxyServer proxy, Logger logger) {
        this.proxyServer = proxy;
        this.logger = logger;
    }

    @Subscribe
    public void onProxyInitialize(ProxyInitializeEvent event) {
        createFile("config.yml");
        createFile("languages/en-US.yml");
        settingsHandler = new SettingsHandler(this);
        UpdateChecker.runCheck(this, settingsHandler.getSettingsManager());
        queueHandler = new QueueHandler(settingsHandler.getSettingsManager(), this);
        queueHandler.enableQueues();
        commandManager = new BungeeCommandManager(this);
        acfHandler = new ACFHandler(this, commandManager);
        proxyServer.getEventManager().register(this, new ConnectionListener(this));
    }

    /**
     * Create a file to be used in the plugin
     * @param name the name of the file
     */
    private void createFile(String name) {
        File folder = dataFolder.toFile();

        if (!folder.exists()) {
            folder.mkdir();
        }
        File languageFolder = new File(folder, "languages");
        if (!languageFolder.exists()) {
            languageFolder.mkdirs();
        }

        File file = new File(folder, name);

        if (!file.exists()) {
            try (InputStream in = getClass().getClassLoader().getResourceAsStream(name)) {
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
}
