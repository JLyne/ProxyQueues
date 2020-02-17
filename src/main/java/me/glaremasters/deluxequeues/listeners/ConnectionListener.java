package me.glaremasters.deluxequeues.listeners;

import ch.jalu.configme.SettingsManager;
import com.velocitypowered.api.event.PostOrder;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.player.ServerConnectedEvent;
import com.velocitypowered.api.event.player.ServerPreConnectEvent;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import me.glaremasters.deluxequeues.DeluxeQueues;
import me.glaremasters.deluxequeues.configuration.sections.ConfigOptions;
import me.glaremasters.deluxequeues.queues.DeluxeQueue;
import me.glaremasters.deluxequeues.queues.QueueHandler;
import me.glaremasters.deluxequeues.queues.QueuePlayer;
import net.kyori.text.TextComponent;
import net.kyori.text.format.TextColor;

import java.util.Optional;

/**
 * Created by Glare
 * Date: 7/14/2019
 * Time: 12:13 AM
 */
public class ConnectionListener {

    private final DeluxeQueues deluxeQueues;
    private final QueueHandler queueHandler;
    private final SettingsManager settingsManager;
    private RegisteredServer waitingServer;

    public ConnectionListener(DeluxeQueues deluxeQueues) {
        this.deluxeQueues = deluxeQueues;
        this.queueHandler = deluxeQueues.getQueueHandler();
        this.settingsManager = deluxeQueues.getSettingsHandler().getSettingsManager();

        String waitingServerName = deluxeQueues.getSettingsHandler().getSettingsManager().getProperty(ConfigOptions.WAITING_SERVER);
        waitingServer = deluxeQueues.getProxyServer().getServer(waitingServerName).orElse(null);
    }

    @Subscribe(order = PostOrder.EARLY)
    public void onJoin(ServerPreConnectEvent event) {
        // Get the server in the event
        RegisteredServer server = event.getOriginalServer();
        // Get the player in the event
        Player player = event.getPlayer();

        RegisteredServer redirected = event.getResult().getServer().orElse(null);

        // Create a boolean for bypass with staff
        boolean bypass = player.hasPermission(settingsManager.getProperty(ConfigOptions.STAFF_PERMISSION));

        if(redirected != null && !redirected.equals(server)) {
            server = redirected;
        }

        // Run this if they dont bypass
        if (bypass) {
            return;
        }

        // Check if the server has a queue
        if(!queueHandler.checkForQueue(server)) {
            return;
        }

        // Get the queue
        DeluxeQueue queue = queueHandler.getQueue(server);
        QueuePlayer queuePlayer = queue.getFromProxy(player);

        if(queuePlayer != null) {
            if(!queuePlayer.isReadyToMove()) {
                event.setResult(ServerPreConnectEvent.ServerResult.denied());
            }
        } else if (queue.canAddPlayer()) {
            if(!event.getPlayer().getCurrentServer().isPresent()) {
                if(waitingServer != null) {
                    event.setResult(ServerPreConnectEvent.ServerResult.allowed(waitingServer));
                } else {
                    player.disconnect(TextComponent.of(
                            "This server has queueing enabled and can't be connected to directly. Please connect via minecraft.rtgame.co.uk")
                                              .color(TextColor.RED));
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
            DeluxeQueue queue = queueHandler.getQueue(event.getServer());

            if(queue != null) {
                queue.removePlayer(event.getPlayer());
            }
        }
    }

    @Subscribe(order = PostOrder.EARLY)
    public void onLeave(DisconnectEvent event) {
        // Remove player from all queues
        queueHandler.clearPlayer(event.getPlayer());
    }

}
