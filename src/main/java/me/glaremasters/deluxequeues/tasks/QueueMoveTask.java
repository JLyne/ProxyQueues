package me.glaremasters.deluxequeues.tasks;

import com.velocitypowered.api.proxy.server.RegisteredServer;
import me.glaremasters.deluxequeues.queues.DeluxeQueue;
import me.glaremasters.deluxequeues.queues.QueuePlayer;

/**
 * Created by Glare
 * Date: 7/13/2019
 * Time: 10:47 PM
 */
public class QueueMoveTask implements Runnable {

    private final DeluxeQueue queue;
    private final RegisteredServer server;

    public QueueMoveTask(DeluxeQueue queue, RegisteredServer server) {
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
        queue.getQueue().forEach(queue::notifyPlayer);

        // Check if the max amount of players on the server are the max slots
        if (queue.getServer().getPlayersConnected().size() >= queue.getMaxSlots()) {
            return;
        }

        // Get the player next in line
        QueuePlayer player = queue.getQueue().getFirst();

        if(player != null && !player.getPlayer().isActive()) {
            queue.removePlayer(player);
        }

        // Make sure the player exists
        if (player != null) {
            player.getPlayer().createConnectionRequest(server).fireAndForget();
            player.setReadyToMove(true);
        }
    }
}
