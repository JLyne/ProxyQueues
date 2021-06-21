package uk.co.notnull.proxyqueues;

import cloud.commandframework.CommandManager;
import cloud.commandframework.annotations.AnnotationParser;
import cloud.commandframework.execution.CommandExecutionCoordinator;
import cloud.commandframework.meta.SimpleCommandMeta;
import cloud.commandframework.minecraft.extras.MinecraftExceptionHandler;
import cloud.commandframework.minecraft.extras.MinecraftHelp;
import cloud.commandframework.velocity.VelocityCommandManager;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.plugin.Dependency;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.PluginContainer;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import ninja.leaping.configurate.ConfigurationNode;
import ninja.leaping.configurate.yaml.YAMLConfigurationLoader;
import uk.co.notnull.proxyqueues.configuration.SettingsHandler;
import uk.co.notnull.proxyqueues.configuration.sections.ConfigOptions;
import uk.co.notnull.proxyqueues.messages.Messages;
import uk.co.notnull.proxyqueues.metrics.MetricsHandler;
import uk.co.notnull.proxyqueues.queues.QueueHandler;
import org.slf4j.Logger;

import javax.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

@Plugin(id="proxyqueues", name="ProxyQueues", dependencies = {
        @Dependency(id="velocity-prometheus-exporter", optional = true),
        @Dependency(id="platform-detection", optional = true),
})
public final class ProxyQueues {

    private static ProxyQueues instance;
    private SettingsHandler settingsHandler;
    private QueueHandler queueHandler;
    private static final LegacyComponentSerializer legacyComponentSerializer = LegacyComponentSerializer.legacyAmpersand();

    private final ProxyServer proxyServer;
    private final Logger logger;

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
        createFile("messages.yml");

        //Message config
        ConfigurationNode messagesConfiguration;

        try {
			messagesConfiguration = YAMLConfigurationLoader.builder().setFile(
					new File(dataFolder.toAbsolutePath().toString(), "messages.yml")).build().load();
		    Messages.set(messagesConfiguration);
		} catch (IOException e) {
			logger.error("Error loading messages.yml");
		}

        settingsHandler = new SettingsHandler(this);
        startQueues();

        Optional<PluginContainer> prometheusExporter = proxyServer.getPluginManager().getPlugin("velocity-prometheus-exporter");

        prometheusExporter.flatMap(PluginContainer::getInstance).ifPresent(instance -> new MetricsHandler(this));

        initCommands();
    }

    public void initCommands() {
        CommandManager<CommandSource> manager = new VelocityCommandManager<>(
                proxyServer.getPluginManager().fromInstance(this).get(),
                proxyServer,
                CommandExecutionCoordinator.simpleCoordinator(),
                Function.identity(),
                Function.identity());

        new MinecraftExceptionHandler<CommandSource>()
            .withArgumentParsingHandler()
            .withInvalidSenderHandler()
            .withInvalidSyntaxHandler()
            .withNoPermissionHandler()
            .withCommandExecutionHandler()
            .withDecorator(message -> message)
            .apply(manager, p -> p);

        new MinecraftHelp<>("/queue help", p -> p, manager);

        AnnotationParser<CommandSource> annotationParser = new AnnotationParser<>(
                manager,
                CommandSource.class,
                parameters -> SimpleCommandMeta.empty()
        );

        annotationParser.parse(new Commands(this));
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

    public void sendMessage(CommandSource player, MessageType messageType, String message) {
        sendMessage(player, messageType, message, Collections.emptyMap());
    }

    public void sendMessage(CommandSource player, MessageType messageType, String message, Map<String, String> replacements) {
        String result = Messages.get("prefix.info");

        if(messageType.equals(MessageType.ERROR)) {
            result = Messages.get("prefix.error");
        }

        player.sendMessage(legacyComponentSerializer.deserialize(result + Messages.get(message, replacements)));
    }
}
