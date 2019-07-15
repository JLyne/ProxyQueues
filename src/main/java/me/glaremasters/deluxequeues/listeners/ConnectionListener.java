package me.glaremasters.deluxequeues.listeners;

import ch.jalu.configme.SettingsManager;
import me.glaremasters.deluxequeues.DeluxeQueues;
import me.glaremasters.deluxequeues.configuration.sections.ConfigOptions;
import me.glaremasters.deluxequeues.queues.DeluxeQueue;
import me.glaremasters.deluxequeues.queues.QueueHandler;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.PlayerDisconnectEvent;
import net.md_5.bungee.api.event.ServerConnectEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;
import net.md_5.bungee.event.EventPriority;

/**
 * Created by Glare
 * Date: 7/14/2019
 * Time: 12:13 AM
 */
public class ConnectionListener implements Listener {

    private DeluxeQueues deluxeQueues;
    private QueueHandler queueHandler;
    private SettingsManager settingsManager;

    public ConnectionListener(DeluxeQueues deluxeQueues) {
        this.deluxeQueues = deluxeQueues;
        this.queueHandler = deluxeQueues.getQueueHandler();
        this.settingsManager = deluxeQueues.getSettingsHandler().getSettingsManager();
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onJoin(ServerConnectEvent event) {
        // Don't go further if they are just joining the proxy
        if (event.getReason() == ServerConnectEvent.Reason.JOIN_PROXY) {
            return;
        }
        // Get the server in the event
        ServerInfo server = event.getTarget();
        // Get the player in the event
        ProxiedPlayer player = event.getPlayer();
        // Create a boolean for bypass with donator
        boolean bypass = player.hasPermission(settingsManager.getProperty(ConfigOptions.DONATOR_PERMISSION));
        // Run this if they dont bypass
        if (!bypass) {
            // Check if the server has a queue
            if (queueHandler.checkForQueue(server)) {
                // Get the queue
                DeluxeQueue queue = queueHandler.getQueue(server);
                // Make sure it doesn't contain the player
                if (!queue.checkForPlayer(player)) {
                    // Make sure the player can actually be added
                    if (queue.canAddPlayer()) {
                        // Cancel the event so they don't go right away
                        event.setCancelled(true);
                        // Add the player to the queue
                        queue.addPlayer(player);
                    }
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onLeave(PlayerDisconnectEvent event) {
        // Remove player from all queues
        queueHandler.clearPlayer(event.getPlayer());
    }

}
