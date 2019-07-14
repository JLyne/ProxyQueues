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

    @EventHandler
    public void onJoin(ServerConnectEvent event) {
        // Get the server in the event
        ServerInfo server = event.getTarget();
        // Get the player in the event
        ProxiedPlayer player = event.getPlayer();
        // Bypass if they are a donator
        if (player.hasPermission(settingsManager.getProperty(ConfigOptions.DONATOR_PERMISSION))) {
            return;
        }
        // Check if the server has a queue
        if (queueHandler.checkForQueue(server)) {
            // Get the queue
            DeluxeQueue queue = queueHandler.getQueue(server);
            // Make sure it doesn't contain the player
            if (!queue.getQueue().contains(player)) {
                // Make sure they aren't joining the proxy for the first time
                if (event.getReason() != ServerConnectEvent.Reason.JOIN_PROXY) {
                    // Cancel the event so they don't go right away
                    event.setCancelled(true);
                    // Add the player to the queue
                    queue.addPlayer(player);
                }
            }
        }
    }

    @EventHandler
    public void onLeave(PlayerDisconnectEvent event) {
        // Remove player from all queues
        queueHandler.clearPlayer(event.getPlayer());
    }

}
