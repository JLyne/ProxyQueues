package uk.co.notnull.proxyqueues;

import co.aikar.commands.CommandIssuer;
import co.aikar.commands.MessageType;
import co.aikar.commands.VelocityCommandManager;
import co.aikar.commands.VelocityMessageFormatter;
import com.mojang.brigadier.Command;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.plugin.Dependency;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.PluginContainer;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import de.sldk.mc.core.MetricRegistry;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.text.format.TextColor;
import uk.co.notnull.proxyqueues.acf.ACFHandler;
import uk.co.notnull.proxyqueues.configuration.SettingsHandler;
import uk.co.notnull.proxyqueues.configuration.sections.ConfigOptions;
import uk.co.notnull.proxyqueues.messages.Messages;
import uk.co.notnull.proxyqueues.metrics.PlayersQueued;
import uk.co.notnull.proxyqueues.queues.QueueHandler;
import org.slf4j.Logger;

import javax.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

@Plugin(id="proxyqueues", name="ProxyQueues", dependencies = {
        @Dependency(id="velocity-prometheus-exporter", optional = true),
        @Dependency(id="floodgate", optional = true)
})
public final class ProxyQueues {

    private static ProxyQueues instance;
    private VelocityCommandManager commandManager;
    private SettingsHandler settingsHandler;
    private QueueHandler queueHandler;

    private final ProxyServer proxyServer;
    private final Logger logger;
    private final LegacyComponentSerializer serializer = LegacyComponentSerializer.legacyAmpersand();

    @Inject
    @DataDirectory
    private Path dataFolder;

    @Inject
    public ProxyQueues(ProxyServer proxy, Logger logger) {
        this.proxyServer = proxy;
        this.logger = logger;
        instance = this;
    }

    @Subscribe
    public void onProxyInitialize(ProxyInitializeEvent event) {
        createFile("config.yml");
        createFile("languages/en-US.yml");
        settingsHandler = new SettingsHandler(this);
        startQueues();
        commandManager = new VelocityCommandManager(proxyServer, this);
        new ACFHandler(this, commandManager);

        commandManager.setFormat(MessageType.INFO, new VelocityMessageFormatter(
                TextColor.YELLOW, TextColor.GREEN, TextColor.LIGHT_PURPLE));

        Optional<PluginContainer> prometheusExporter = proxyServer.getPluginManager().getPlugin("velocity-prometheus-exporter");
        prometheusExporter.flatMap(PluginContainer::getInstance).ifPresent(instance -> {
			PlayersQueued playersQueued = new PlayersQueued(this);
		    MetricRegistry.getInstance().register(playersQueued);

		    playersQueued.enable();
		});

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

    public VelocityCommandManager getCommandManager() {
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
    }

    public ProxyServer getProxyServer() {
        return proxyServer;
    }

    public Logger getLogger() {
        return logger;
    }

    public File getDataFolder() {
        return dataFolder.toFile();
    }

    public Optional<RegisteredServer> getWaitingServer() {
        String waitingServerName = getSettingsHandler().getSettingsManager().getProperty(ConfigOptions.WAITING_SERVER);
        return getProxyServer().getServer(waitingServerName);
    }

    public static ProxyQueues getInstance() {
        return instance;
    }

    public void sendMessage(CommandIssuer player, MessageType messageType, Messages message, String... replacements) {
        sendMessage((Player) player.getIssuer(), messageType, message, replacements);
    }

    public void sendMessage(Player player, MessageType messageType, Messages message, String... replacements) {
        CommandIssuer issuer = getCommandManager().getCommandIssuer(player);
        String prefix = commandManager.formatMessage(issuer, messageType, Messages.PREFIX__INFO);

        if(messageType.equals(MessageType.ERROR)) {
            prefix = commandManager.formatMessage(issuer, messageType, Messages.PREFIX__ERROR);
        }

        String result = prefix + commandManager.formatMessage(issuer, messageType, message, replacements);
        player.sendMessage(serializer.deserialize(result));
    }
}
