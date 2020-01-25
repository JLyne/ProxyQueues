package me.glaremasters.deluxequeues.tasks;

import com.velocitypowered.api.proxy.ConnectionRequestBuilder;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import me.glaremasters.deluxequeues.queues.DeluxeQueue;

/**
 * Created by Glare
 * Date: 7/13/2019
 * Time: 10:47 PM
 */
public class QueueMoveTask implements Runnable {

    private DeluxeQueue queue;
    private RegisteredServer server;

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

        // Persist the actionbar
        if (queue.getNotifyMethod().toLowerCase().equalsIgnoreCase("actionbar")) {
            queue.getQueue().forEach(p -> queue.notifyPlayer(p));
        }

        // Check if the max amount of players on the server are the max slots
        if (queue.getServer().getPlayersConnected().size() >= queue.getMaxSlots()) {
            return;
        }

        // Get the player next in line
        Player player = queue.getQueue().getFirst();
        // Make sure the player exists
        if (player != null) {
            player.createConnectionRequest(server).fireAndForget();
        }
    }
}
