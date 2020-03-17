package me.glaremasters.deluxequeues.listeners;

import ch.jalu.configme.SettingsManager;
import com.velocitypowered.api.event.PostOrder;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.player.KickedFromServerEvent;
import com.velocitypowered.api.event.player.ServerConnectedEvent;
import com.velocitypowered.api.event.player.ServerPreConnectEvent;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import me.glaremasters.deluxequeues.DeluxeQueues;
import me.glaremasters.deluxequeues.QueueType;
import me.glaremasters.deluxequeues.configuration.sections.ConfigOptions;
import me.glaremasters.deluxequeues.queues.DeluxeQueue;
import me.glaremasters.deluxequeues.queues.QueueHandler;
import me.glaremasters.deluxequeues.queues.QueuePlayer;
import net.kyori.text.Component;
import net.kyori.text.TextComponent;
import net.kyori.text.format.TextColor;
import net.kyori.text.serializer.plain.PlainComponentSerializer;

import java.util.List;
import java.util.Optional;

/**
 * Created by Glare
 * Date: 7/14/2019
 * Time: 12:13 AM
 */
public class ConnectionListener {

    private final QueueHandler queueHandler;
    private final List<String> fatalErrors;
    private final SettingsManager settingsManager;
    private DeluxeQueues deluxeQueues;
    private RegisteredServer waitingServer;

    public ConnectionListener(DeluxeQueues deluxeQueues) {
        settingsManager = deluxeQueues.getSettingsHandler().getSettingsManager();

        this.queueHandler = deluxeQueues.getQueueHandler();
        this.deluxeQueues = deluxeQueues;
        this.fatalErrors = deluxeQueues.getSettingsHandler().getSettingsManager().getProperty(
                            ConfigOptions.FATAL_ERRORS);

        String waitingServerName = settingsManager.getProperty(ConfigOptions.WAITING_SERVER);
        waitingServer = deluxeQueues.getProxyServer().getServer(waitingServerName).orElse(null);
    }

    @Subscribe(order = PostOrder.LATE)
    public void onJoin(ServerPreConnectEvent event) {
        if(!event.getResult().isAllowed()) {
            return;
        }

        // Get the server in the event
        RegisteredServer server = event.getOriginalServer();
        // Get the player in the event
        Player player = event.getPlayer();

        RegisteredServer redirected = event.getResult().getServer().orElse(null);

        if(redirected != null && !redirected.equals(server)) {
            server = redirected;
        }

        // Check if the server has a queue
        if(!queueHandler.checkForQueue(server)) {
            return;
        }

        // Get the queue
        DeluxeQueue queue = queueHandler.getQueue(server);
        Optional<QueuePlayer> queuePlayer = queue.getQueuePlayer(player);

        if(queuePlayer.isPresent()) {
            if(!queuePlayer.get().isConnecting()) {
                event.setResult(ServerPreConnectEvent.ServerResult.denied());
            }
        } else if (queue.isActive()) {
            if(!event.getPlayer().getCurrentServer().isPresent()) {
                if(waitingServer != null) {
                    event.setResult(ServerPreConnectEvent.ServerResult.allowed(waitingServer));
                } else {
                    player.disconnect(TextComponent.of(
                            "This server has queueing enabled and can't be connected to directly. Please connect via minecraft.rtgame.co.uk")
                                              .color(TextColor.RED));

                    return;
                }
            } else {
                // Cancel the event so they don't go right away
                event.setResult(ServerPreConnectEvent.ServerResult.denied());
            }

            // Add the player to the queue
            queue.addPlayer(player);
        }
    }

    @Subscribe(order = PostOrder.LATE)
    public void onConnected(ServerConnectedEvent event) {
        if(!event.getServer().equals(waitingServer)) {
            DeluxeQueue targetQueue = queueHandler.getQueue(event.getServer());

            if(targetQueue != null) {
                targetQueue.removePlayer(event.getPlayer(), true);
            }

        }

        DeluxeQueue previousQueue = queueHandler.getQueue(event.getServer());

        if(previousQueue != null) {
            previousQueue.cleanupConnectedState(event.getPlayer());
        }
    }

    @Subscribe(order = PostOrder.EARLY)
    public void onLeave(DisconnectEvent event) {
        // Remove player from all queues
        queueHandler.clearPlayer(event.getPlayer());
    }

    @Subscribe(order = PostOrder.LAST)
    public void onKick(KickedFromServerEvent event) {
        deluxeQueues.getLogger()
                .info("Player " + event.getPlayer().getUsername() + " kicked from " +
                              event.getServer().getServerInfo().getName() + ". Reason: " + event.getOriginalReason());

        if(event.kickedDuringServerConnect()) {
            deluxeQueues.getLogger().info("Kicked during connect");
            return;
        }

        if(!event.getServer().equals(waitingServer)) {
            DeluxeQueue queue = queueHandler.getQueue(event.getServer());

            if(queue == null || !queue.isActive()) {
                deluxeQueues.getLogger().info("No queue active");
                return;
            }

            Component reason = event.getOriginalReason().orElse(TextComponent.empty());
            String reasonPlain = PlainComponentSerializer.INSTANCE.serialize(reason);

            boolean fatal = fatalErrors.stream().anyMatch(reasonPlain::contains);

            if(!fatal) {
                deluxeQueues.getLogger()
                .info("Reason not fatal, requeueing " + event.getPlayer().getUsername());

                boolean staff = event.getPlayer()
                        .hasPermission(settingsManager.getProperty(ConfigOptions.STAFF_PERMISSION));

                queue.addPlayer(event.getPlayer(), staff ? QueueType.STAFF : QueueType.PRIORITY);
            } else {
                deluxeQueues.getLogger().info("Reason fatal");
            }
        }
    }

}
