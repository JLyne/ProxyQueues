package me.glaremasters.deluxequeues;

import co.aikar.commands.BungeeCommandManager;
import lombok.Getter;
import me.glaremasters.deluxequeues.configuration.SettingsHandler;
import net.md_5.bungee.api.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;

@Getter
public final class DeluxeQueues extends Plugin {

    private BungeeCommandManager commandManager;
    private SettingsHandler settingsHandler;

    @Override
    public void onEnable() {
        createFile("config.yml");
        settingsHandler = new SettingsHandler(this);
        loadACF();
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }

    private void loadACF() {
        // Load the command manager
        commandManager = new BungeeCommandManager(this);
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
}
