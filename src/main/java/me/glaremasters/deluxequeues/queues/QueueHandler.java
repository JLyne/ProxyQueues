package me.glaremasters.deluxequeues.queues;

import net.md_5.bungee.api.connection.ProxiedPlayer;
import org.jetbrains.annotations.NotNull;
import lombok.Getter;
import net.md_5.bungee.api.config.ServerInfo;

import java.util.List;

/**
 * Created by Glare
 * Date: 7/13/2019
 * Time: 10:31 PM
 */
@Getter
public class QueueHandler {

    private List<DeluxeQueue> queues;
    private List<ServerInfo> servers;

    public QueueHandler(List<DeluxeQueue> queues, List<ServerInfo> servers) {
        this.queues = queues;
        this.servers = servers;
    }

    /**
     * Create a new queue for a server
     * @param queue the new queue
     */
    public void createQueue(@NotNull DeluxeQueue queue) {
        if (!queues.contains(queue)) {
            servers.add(queue.getServer());
            queues.add(queue);
        }
    }

    /**
     * Delete a queue if it exists
     * @param queue the queue to check
     */
    public void deleteQueue(@NotNull DeluxeQueue queue) {
        if (queues.contains(queue)) {
            servers.remove(queue.getServer());
            queues.remove(queue);
        }
    }

    /**
     * Check if a queue exists
     * @param queue the queue to check
     * @return queue object
     */
    public DeluxeQueue getQueue(@NotNull DeluxeQueue queue) {
        return queues.stream().filter(q -> q.equals(queue)).findFirst().orElse(null);
    }

    /**
     * Get a queue from it's server
     * @param server the server to get the queue from
     * @return the queue
     */
    public DeluxeQueue getQueue(@NotNull ServerInfo server) {
        return queues.stream().filter(q -> q.getServer().equals(server)).findFirst().orElse(null);
    }

    /**
     * Remove a player from all queues
     * @param player the player to remove
     */
    public void clearPlayer(ProxiedPlayer player) {
        queues.forEach(q -> q.getQueue().remove(player));
    }

    /**
     * Check if a server has a queue
     * @param server the server to check
     * @return if the server has a queue or not
     */
    public boolean checkForQueue(ServerInfo server) {
        return servers.contains(server);
    }
}
