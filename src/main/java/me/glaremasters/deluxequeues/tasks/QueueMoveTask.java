package me.glaremasters.deluxequeues.tasks;

import me.glaremasters.deluxequeues.DeluxeQueues;
import me.glaremasters.deluxequeues.queues.DeluxeQueue;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;

/**
 * Created by Glare
 * Date: 7/13/2019
 * Time: 10:47 PM
 */
public class QueueMoveTask implements Runnable {

    private DeluxeQueues deluxeQueues;
    private DeluxeQueue queue;
    private ServerInfo server;

    public QueueMoveTask(DeluxeQueues deluxeQueues, DeluxeQueue queue, ServerInfo server) {
        this.deluxeQueues = deluxeQueues;
        this.queue = queue;
        this.server = server;
    }

    @Override
    public void run() {
        if (queue.getQueue().isEmpty()) {
            return;
        }

        ProxiedPlayer player = (ProxiedPlayer) queue.getQueue().element();
        if (player != null) {
            player.connect(server);
        }
        queue.getQueue().remove(player);
    }
}
