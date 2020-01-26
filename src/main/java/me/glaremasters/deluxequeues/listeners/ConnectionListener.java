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
import me.glaremasters.deluxequeues.events.PlayerQueueEvent;
import me.glaremasters.deluxequeues.queues.DeluxeQueue;
import me.glaremasters.deluxequeues.queues.QueueHandler;
import net.kyori.text.TextComponent;
import net.kyori.text.format.TextColor;

import java.util.Optional;

/**
 * Created by Glare
 * Date: 7/14/2019
 * Time: 12:13 AM
 */
public class ConnectionListener {

    private DeluxeQueues deluxeQueues;
    private QueueHandler queueHandler;
    private SettingsManager settingsManager;

    public ConnectionListener(DeluxeQueues deluxeQueues) {
        this.deluxeQueues = deluxeQueues;
        this.queueHandler = deluxeQueues.getQueueHandler();
        this.settingsManager = deluxeQueues.getSettingsHandler().getSettingsManager();
    }

    @Subscribe(order = PostOrder.EARLY)
    public void onJoin(ServerPreConnectEvent event) {
        // Get the server in the event
        RegisteredServer server = event.getOriginalServer();
        // Get the player in the event
        Player player = event.getPlayer();
        // Create a boolean for bypass with donator
        boolean bypass = player.hasPermission(settingsManager.getProperty(ConfigOptions.DONATOR_PERMISSION));

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

        int queuePosition = queue.checkForPlayer(player);

        switch (queuePosition) {
            case -1 : //Not in queue
                 // Make sure the player can actually be added
                if (queue.canAddPlayer()) {
                    if(!event.getPlayer().getCurrentServer().isPresent()) {
                        String waitingServerName = deluxeQueues.getSettingsHandler().getSettingsManager().getProperty(ConfigOptions.WAITING_SERVER);
                        Optional<RegisteredServer> waitingServer = deluxeQueues.getProxyServer().getServer(waitingServerName);

                        if(waitingServer.isPresent()) {
                            event.setResult(ServerPreConnectEvent.ServerResult.allowed(waitingServer.get()));
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

            case 0 : //Front of queue
                break;

            default: //Elsewhere in queue
                event.setResult(ServerPreConnectEvent.ServerResult.denied());

        }
    }

    @Subscribe(order = PostOrder.EARLY)
    public void onConnected(ServerConnectedEvent event) {
        queueHandler.clearPlayer(event.getPlayer());
    }

    @Subscribe(order = PostOrder.EARLY)
    public void onLeave(DisconnectEvent event) {
        // Remove player from all queues
        queueHandler.clearPlayer(event.getPlayer());
    }

}
