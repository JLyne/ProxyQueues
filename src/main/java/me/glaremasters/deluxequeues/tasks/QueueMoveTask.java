package me.glaremasters.deluxequeues.tasks;

import me.glaremasters.deluxequeues.queues.DeluxeQueue;
import net.md_5.bungee.api.ServerConnectRequest;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.ServerConnectEvent;

/**
 * Created by Glare
 * Date: 7/13/2019
 * Time: 10:47 PM
 */
public class QueueMoveTask implements Runnable {

    private DeluxeQueue queue;
    private ServerInfo server;

    public QueueMoveTask(DeluxeQueue queue, ServerInfo server) {
        this.queue = queue;
        this.server = server;
    }

    @Override
    public void run() {
        // Make sure the queue isn't empty
        if (queue.getQueue().isEmpty()) {
            return;
        }

        // Persist the actionbar
        if (queue.getNotifyMethod().toLowerCase().equalsIgnoreCase("actionbar")) {
            queue.getQueue().forEach(p -> queue.notifyPlayer(p));
        }

        // Check if the max amount of players on the server are the max slots
        if (queue.getServer().getPlayers().size() >= queue.getMaxSlots()) {
            return;
        }
        // Get the player next in line
        ProxiedPlayer player = queue.getQueue().getFirst();
        // Make sure the player exists
        if (player != null) {
            ServerConnectRequest.Builder request = ServerConnectRequest.builder().retry(false).reason(
                    ServerConnectEvent.Reason.PLUGIN).target(server).silent(true);

            queue.getDeluxeQueues().getLogger().info(request.toString());

            player.connect(request.build());
        }
    }
}
