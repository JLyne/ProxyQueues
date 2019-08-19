package me.glaremasters.deluxequeues;

import co.aikar.commands.BungeeCommandManager;
import me.glaremasters.deluxequeues.acf.ACFHandler;
import me.glaremasters.deluxequeues.commands.CommandLeave;
import me.glaremasters.deluxequeues.configuration.SettingsHandler;
import me.glaremasters.deluxequeues.listeners.ConnectionListener;
import me.glaremasters.deluxequeues.queues.QueueHandler;
import me.glaremasters.deluxequeues.updater.UpdateChecker;
import net.md_5.bungee.api.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.JarURLConnection;
import java.nio.file.Files;
import java.util.Enumeration;
import java.util.Objects;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public final class DeluxeQueues extends Plugin {

    private BungeeCommandManager commandManager;
    private SettingsHandler settingsHandler;
    private QueueHandler queueHandler;
    private ACFHandler acfHandler;

    @Override
    public void onEnable() {
        createFile("config.yml");
        settingsHandler = new SettingsHandler(this);
        UpdateChecker.runCheck(this, settingsHandler.getSettingsManager());
        queueHandler = new QueueHandler(settingsHandler.getSettingsManager(), this);
        queueHandler.enableQueues();
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

        File file = new File(getDataFolder(), name);

        if (!file.exists()) {
            try (InputStream in = getResourceAsStream(name)) {
                Files.copy(in, file.toPath());
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }

    /**
     * Save and handle new files if needed
     */
    private void saveData() {
        File languageFolder = new File(getDataFolder(), "languages");
        if (!languageFolder.exists()) //noinspection ResultOfMethodCallIgnored
            languageFolder.mkdirs();
        try {
            final JarURLConnection connection = (JarURLConnection) Objects.requireNonNull(this.getClass().getClassLoader().getResource("languages")).openConnection();
            final JarFile thisJar = connection.getJarFile();
            final Enumeration<JarEntry> entries = thisJar.entries();
            while (entries.hasMoreElements()) {
                final JarEntry current = entries.nextElement();
                if (!current.getName().startsWith("languages/") || current.getName().length() == "languages/".length()) {
                    continue;
                }
                final String name = current.getName().substring("languages/".length());
                File langFile = new File(languageFolder, name);
                if (!langFile.exists()) {
                    try (InputStream in = getResourceAsStream(name)) {
                        Files.copy(in, langFile.toPath());
                    } catch (IOException ex) {
                        ex.printStackTrace();
                    }
                }
            }

        } catch (final IOException ex) {
            ex.printStackTrace();
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
