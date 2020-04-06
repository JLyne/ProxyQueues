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

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by Glare
 * Date: 7/13/2019
 * Time: 10:31 PM
 */
public class QueueHandler {

    private final ConcurrentHashMap<RegisteredServer, DeluxeQueue> queues;
    private final SettingsManager settingsManager;
    private final DeluxeQueues deluxeQueues;

    public QueueHandler(SettingsManager settingsManager, DeluxeQueues deluxeQueues) {
        this.settingsManager = settingsManager;
        this.deluxeQueues = deluxeQueues;
        this.queues = new ConcurrentHashMap<>();

        updateQueues();
    }

    /**
     * Create a new queue for a server
     * @param server The server the new queue is for
     * @return DeluxeQueue - The queue
     */
    public DeluxeQueue createQueue(@NotNull RegisteredServer server, int requiredPlayers, int maxNormal, int maxPriority, int maxStaff) {
        return queues.compute(server, (s, queue) -> {
            if(queue == null) {
                deluxeQueues.getLogger().info("Creating queue for " + server.getServerInfo().getName());
                return new DeluxeQueue(deluxeQueues, s, requiredPlayers, maxNormal, maxPriority, maxStaff);
            } else {
                deluxeQueues.getLogger().info("Updating queue for " + server.getServerInfo().getName());
                queue.setPlayersRequired(requiredPlayers);
                queue.setMaxSlots(maxNormal);
                queue.setPriorityMaxSlots(maxPriority);
                queue.setStaffMaxSlots(maxStaff);

                return queue;
            }
        });
    }

    /**
     * Delete a queue if it exists
     * @param server The server to remove the queue for
     */
    public void deleteQueue(@NotNull RegisteredServer server) {
        queues.computeIfPresent(server, (s, queue) -> {
            queue.destroy();
            return null;
        });
    }

    /**
     * Get a queue from it's server
     * @param server the server to get the queue for
     * @return the queue
     */
    public DeluxeQueue getQueue(@NotNull RegisteredServer server) {
        return queues.get(server);
    }

    /**
     * Get a queue from it's server
     * @param player the server to get the queue from
     * @return the queue
     */
    public Optional<DeluxeQueue> getCurrentQueue(@NotNull Player player) {
        return queues.values().stream()
                .filter(q -> q.isPlayerQueued(player)).findFirst();
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
        queues.forEach((server, queue) -> queue.removePlayer(player, false));

        if(silent) {
            return;
        }

        if(!player.isActive()) {
            return;
        }

        RegisteredServer waitingServer = deluxeQueues.getWaitingServer().orElse(null);
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
        clearPlayer(player);

        RegisteredServer waitingServer = deluxeQueues.getWaitingServer().orElse(null);
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
     * Updates all queues from the config, creating/updating/deleting as required
     */
    public void updateQueues() {
        ArrayList<RegisteredServer> queuedServers = new ArrayList<>();

        settingsManager.getProperty(ConfigOptions.QUEUE_SERVERS).forEach(s -> {
            try {
                String[] split = s.split(";");
                String serverName = split[0];

                int requiredPlayers = Integer.parseInt(split[1]),
                    maxNormal = Integer.parseInt(split[2]),
                    maxPriority = Integer.parseInt(split[3]),
                    maxStaff = Integer.parseInt(split[4]);

                RegisteredServer server = deluxeQueues.getProxyServer().getServer(serverName).get();

                DeluxeQueue queue = createQueue(server, requiredPlayers, maxNormal, maxPriority, maxStaff);
                queue.setDelayLength(settingsManager.getProperty(ConfigOptions.DELAY_LENGTH));

                queuedServers.add(server);
            } catch (Exception ex) {
                deluxeQueues.getLogger().warn("It seems like one of your servers was configured invalidly in the config.");
                ex.printStackTrace();
            }
        });

        Iterator<Map.Entry<RegisteredServer, DeluxeQueue>> it = queues.entrySet().iterator();

        //Delete queues removed from config
        while(it.hasNext()) {
            Map.Entry<RegisteredServer, DeluxeQueue> pair = it.next();

            if(!queuedServers.contains(pair.getKey())) {
                deluxeQueues.getLogger().info("Deleting queue for " + pair.getKey().getServerInfo().getName());
                pair.getValue().destroy();
                it.remove();
            }
        }
    }

    public Collection<DeluxeQueue> getQueues() {
        return this.queues.values();
    }
}
