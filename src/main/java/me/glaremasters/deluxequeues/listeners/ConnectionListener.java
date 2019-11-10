package me.glaremasters.deluxequeues.listeners;

import ch.jalu.configme.SettingsManager;
import co.aikar.commands.ACFBungeeUtil;
import me.glaremasters.deluxequeues.DeluxeQueues;
import me.glaremasters.deluxequeues.configuration.sections.ConfigOptions;
import me.glaremasters.deluxequeues.events.PlayerQueueEvent;
import me.glaremasters.deluxequeues.queues.DeluxeQueue;
import me.glaremasters.deluxequeues.queues.QueueHandler;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.PlayerDisconnectEvent;
import net.md_5.bungee.api.event.ServerConnectEvent;
import net.md_5.bungee.api.event.ServerConnectedEvent;
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
        // Get the server in the event
        ServerInfo server = event.getTarget();
        // Get the player in the event
        ProxiedPlayer player = event.getPlayer();
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
                    if(event.getReason() == ServerConnectEvent.Reason.JOIN_PROXY) {
                        String waitingServerName = deluxeQueues.getSettingsHandler().getSettingsManager().getProperty(ConfigOptions.WAITING_SERVER);
                        ServerInfo waitingServer = deluxeQueues.getProxy().getServerInfo(waitingServerName);

                        if(waitingServer != null) {
                            event.setTarget(waitingServer);
                        } else {
                            player.disconnect(new ComponentBuilder(
                                    "This server has queueing enabled and can't be connected to directly. Please connect via minecraft.rtgame.co.uk").color(
                                    ChatColor.RED).create());
                        }
                    } else {
                        // Cancel the event so they don't go right away
                        event.setCancelled(true);
                    }

                    // Add the player to the queue
                    queue.addPlayer(player);
                }

            case 0 : //Front of queue
                break;

            default: //Elsewhere in queue
                event.setCancelled(true);

        }
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onConnected(ServerConnectedEvent event) {
        queueHandler.clearPlayer(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onLeave(PlayerDisconnectEvent event) {
        // Remove player from all queues
        queueHandler.clearPlayer(event.getPlayer());
    }

}
