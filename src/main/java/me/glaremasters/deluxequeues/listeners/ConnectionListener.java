package me.glaremasters.deluxequeues.listeners;

import me.glaremasters.deluxequeues.DeluxeQueues;
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

    public ConnectionListener(DeluxeQueues deluxeQueues) {
        this.deluxeQueues = deluxeQueues;
        this.queueHandler = deluxeQueues.getQueueHandler();
    }

    @EventHandler
    public void onJoin(ServerConnectEvent event) {
        ServerInfo server = event.getTarget();
        ProxiedPlayer player = event.getPlayer();
        if (queueHandler.checkForQueue(server)) {
            DeluxeQueue queue = queueHandler.getQueue(server);
            if (!queue.getQueue().contains(player)) {
                event.setCancelled(true);
                queue.addPlayer(player);
            }
        }
    }

    @EventHandler
    public void onLeave(PlayerDisconnectEvent event) {
        queueHandler.clearPlayer(event.getPlayer());
    }

}
