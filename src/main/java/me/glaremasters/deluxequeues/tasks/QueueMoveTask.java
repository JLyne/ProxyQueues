package me.glaremasters.deluxequeues.tasks;

import me.glaremasters.deluxequeues.queues.DeluxeQueue;
import me.glaremasters.deluxequeues.queues.QueuePlayer;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;

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

        // Persist the notification to the user
        queue.getQueue().forEach(p -> queue.notifyPlayer(p));

        // Check if the max amount of players on the server are the max slots
        if (queue.getServer().getPlayers().size() >= queue.getMaxSlots()) {
            return;
        }
        // Get the player next in line
        QueuePlayer player = queue.getQueue().getFirst();
        // Make sure the player exists
        if (player != null) {
            // Move the player to that server
            player.getPlayer().connect(server);
            player.setReadyToMove(true);
        }
/*        // Remove the player from the queue
        queue.getQueue().pollFirst();*/
    }
}
