package me.glaremasters.deluxequeues.queues;

import ch.jalu.configme.SettingsManager;
import me.glaremasters.deluxequeues.DeluxeQueues;
import me.glaremasters.deluxequeues.configuration.sections.ConfigOptions;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Glare
 * Date: 7/13/2019
 * Time: 10:31 PM
 */
public class QueueHandler {

    private List<DeluxeQueue> queues;
    private List<ServerInfo> servers;
    private SettingsManager settingsManager;
    private DeluxeQueues deluxeQueues;

    public QueueHandler(SettingsManager settingsManager, DeluxeQueues deluxeQueues) {
        this.settingsManager = settingsManager;
        this.deluxeQueues = deluxeQueues;
        this.queues = new ArrayList<>();
        this.servers = new ArrayList<>();
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

    /**
     * Enable all the queues on the server
     */
    public void enableQueues() {
        settingsManager.getProperty(ConfigOptions.QUEUE_SERVERS).forEach(s -> {
            try {
                String[] split = s.split(";");
                DeluxeQueue queue = new DeluxeQueue(deluxeQueues, deluxeQueues.getProxy().getServerInfo(split[0]), Integer.valueOf(split[1]), Integer.valueOf(split[2]));
                createQueue(queue);
            } catch (Exception ex) {
                deluxeQueues.getLogger().warning("It seems like one of your servers was configured invalidly in the config.");
            }
        });
    }

    public List<DeluxeQueue> getQueues() {
        return this.queues;
    }

    public List<ServerInfo> getServers() {
        return this.servers;
    }

    public SettingsManager getSettingsManager() {
        return this.settingsManager;
    }

    public DeluxeQueues getDeluxeQueues() {
        return this.deluxeQueues;
    }
}
