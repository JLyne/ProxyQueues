package me.glaremasters.deluxequeues.queues;

import ch.jalu.configme.SettingsManager;
import co.aikar.commands.MessageType;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import me.glaremasters.deluxequeues.DeluxeQueues;
import me.glaremasters.deluxequeues.configuration.sections.ConfigOptions;
import me.glaremasters.deluxequeues.messages.Messages;
import net.kyori.text.TextComponent;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Created by Glare
 * Date: 7/13/2019
 * Time: 10:31 PM
 */
public class QueueHandler {

    private final List<DeluxeQueue> queues;
    private final List<RegisteredServer> servers;
    private final SettingsManager settingsManager;
    private final DeluxeQueues deluxeQueues;

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
    public DeluxeQueue getQueue(@NotNull RegisteredServer server) {
        return queues.stream().filter(q -> q.getServer().equals(server)).findFirst().orElse(null);
    }

    /**
     * Get a queue from it's server
     * @param player the server to get the queue from
     * @return the queue
     */
    public Optional<DeluxeQueue> getCurrentQueue(@NotNull Player player) {
        return queues.stream().filter(q -> q.isPlayerQueued(player)).findFirst();
    }

    /**
     * Remove a player from all queues
     * @param player the player to remove
     */
    public void clearPlayer(Player player) {
        clearPlayer(player, true);
    }

    /**
     * Remove a player from all queues
     * @param player the player to remove
     */
    public void clearPlayer(Player player, boolean silent) {
        queues.forEach(q -> q.removePlayer(player, false));

        if(silent) {
            return;
        }

        if(!player.isActive()) {
            return;
        }

        String waitingServerName = deluxeQueues.getSettingsHandler().getSettingsManager().getProperty(ConfigOptions.WAITING_SERVER);
        RegisteredServer waitingServer = deluxeQueues.getProxyServer().getServer(waitingServerName).orElse(null);

        Optional<ServerConnection> currentServer = player.getCurrentServer();

        if(currentServer.isPresent() && currentServer.get().getServer().equals(waitingServer)) {
            player.disconnect(TextComponent.of(deluxeQueues.getCommandManager()
                                      .formatMessage(deluxeQueues.getCommandManager().getCommandIssuer(player), MessageType.ERROR,
                                                     Messages.COMMANDS__LEAVE_SUCCESS)));
        } else {
            deluxeQueues.getCommandManager().getCommandIssuer(player).sendError(Messages.COMMANDS__LEAVE_SUCCESS);
        }
    }

    public void kickPlayer(Player player) {
        String waitingServerName = deluxeQueues.getSettingsHandler().getSettingsManager().getProperty(ConfigOptions.WAITING_SERVER);
        RegisteredServer waitingServer = deluxeQueues.getProxyServer().getServer(waitingServerName).orElse(null);

        clearPlayer(player);

        Optional<ServerConnection> currentServer = player.getCurrentServer();

        if(currentServer.isPresent() && currentServer.get().getServer().equals(waitingServer)) {
            player.disconnect(TextComponent.of(deluxeQueues.getCommandManager()
                                      .formatMessage(deluxeQueues.getCommandManager().getCommandIssuer(player), MessageType.ERROR,
                                                     Messages.ERRORS__QUEUE_REMOVED)));
        } else {
            deluxeQueues.getCommandManager().getCommandIssuer(player).sendError(Messages.ERRORS__QUEUE_REMOVED);
        }
    }


    /**
     * Check if a server has a queue
     * @param server the server to check
     * @return if the server has a queue or not
     */
    public boolean checkForQueue(RegisteredServer server) {
        return servers.contains(server);
    }

    /**
     * Enable all the queues on the server
     */
    public void enableQueues() {
        settingsManager.getProperty(ConfigOptions.QUEUE_SERVERS).forEach(s -> {
            try {
                String[] split = s.split(";");
                Optional<RegisteredServer> server = deluxeQueues.getProxyServer().getServer(split[0]);
                DeluxeQueue queue = new DeluxeQueue(deluxeQueues, server.get(), Integer.parseInt(split[1]),
                                                    Integer.parseInt(split[2]), Integer.parseInt(split[3]),
                                                    Integer.parseInt(split[4]));
                createQueue(queue);
            } catch (Exception ex) {
                deluxeQueues.getLogger().warn("It seems like one of your servers was configured invalidly in the config.");
                ex.printStackTrace();
            }
        });
    }

    public List<DeluxeQueue> getQueues() {
        return this.queues;
    }

    public List<RegisteredServer> getServers() {
        return this.servers;
    }

    public SettingsManager getSettingsManager() {
        return this.settingsManager;
    }

    public DeluxeQueues getDeluxeQueues() {
        return this.deluxeQueues;
    }
}
