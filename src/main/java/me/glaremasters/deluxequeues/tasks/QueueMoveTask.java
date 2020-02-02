package me.glaremasters.deluxequeues.tasks;

import com.velocitypowered.api.proxy.ConnectionRequestBuilder;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import me.glaremasters.deluxequeues.queues.DeluxeQueue;
import net.md_5.bungee.api.ServerConnectRequest;
import me.glaremasters.deluxequeues.queues.QueuePlayer;
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

        // Persist the notification to the user
        queue.getQueue().forEach(p -> queue.notifyPlayer(p));

        // Check if the max amount of players on the server are the max slots
        if (queue.getServer().getPlayersConnected().size() >= queue.getMaxSlots()) {
            return;
        }

        // Get the player next in line
        QueuePlayer player = queue.getQueue().getFirst();
        // Make sure the player exists
        if (player != null) {
            player.createConnectionRequest(server).fireAndForget();
        }
    }
}
