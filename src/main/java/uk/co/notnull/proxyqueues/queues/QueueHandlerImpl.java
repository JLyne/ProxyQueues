/*
 * ProxyQueues, a Velocity queueing solution
 * Copyright (c) 2021 James Lyne
 *
 * Some portions of this file were taken from https://github.com/darbyjack/DeluxeQueues
 * These portions are Copyright (c) 2019 Glare
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package uk.co.notnull.proxyqueues.queues;

import ch.jalu.configme.SettingsManager;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import uk.co.notnull.proxyqueues.api.MessageType;
import uk.co.notnull.proxyqueues.ProxyQueuesImpl;
import uk.co.notnull.proxyqueues.api.queues.ProxyQueue;
import uk.co.notnull.proxyqueues.api.queues.QueueHandler;
import uk.co.notnull.proxyqueues.configuration.sections.ConfigOptions;
import uk.co.notnull.proxyqueues.Messages;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public final class QueueHandlerImpl implements QueueHandler {

    private final ConcurrentHashMap<RegisteredServer, ProxyQueue> queues;
    private final SettingsManager settingsManager;
    private final ProxyQueuesImpl proxyQueues;

    public QueueHandlerImpl(SettingsManager settingsManager, ProxyQueuesImpl proxyQueues) {
        this.settingsManager = settingsManager;
        this.proxyQueues = proxyQueues;
        this.queues = new ConcurrentHashMap<>();

        updateQueues();
    }

    /**
     * Create a new queue for a server
     * @param server The server the new queue is for
     * @return ProxyQueue - The queue
     */
    public ProxyQueue createQueue(@NotNull RegisteredServer server, int requiredPlayers, int maxNormal, int maxPriority, int maxStaff) {
        return queues.compute(server, (s, queue) -> {
            if(queue == null) {
                proxyQueues.getLogger().info("Creating queue for " + server.getServerInfo().getName());
                return new ProxyQueueImpl(proxyQueues, s, requiredPlayers, maxNormal, maxPriority, maxStaff);
            } else {
                proxyQueues.getLogger().info("Updating queue for " + server.getServerInfo().getName());
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
    public ProxyQueue getQueue(@NotNull RegisteredServer server) {
        return queues.get(server);
    }

    /**
     * Get a queue from it's server
     * @param player the server to get the queue from
     * @return the queue
     */
    public Optional<ProxyQueue> getCurrentQueue(@NotNull Player player) {
        return queues.values().stream()
                .filter(q -> q.isPlayerQueued(player)).findFirst();
    }

    public Optional<ProxyQueue> getCurrentQueue(@NotNull UUID uuid) {
        return queues.values().stream()
                .filter(q -> q.isPlayerQueued(uuid)).findFirst();
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
     * @param uuid the UUID of the player to remove
     */
    public void clearPlayer(UUID uuid) {
        clearPlayer(uuid, true);
    }

    /**
     * Remove a player from all queues
     * @param player The player to remove
     */
    public void clearPlayer(Player player, boolean silent) {
        queues.forEach((server, queue) -> queue.removePlayer(player, false));

        if(silent) {
            return;
        }

        notifyQueueRemoval(player, "commands.leave-success", MessageType.INFO);
    }

    /**
     * Remove a player from all queues
     * @param uuid The UUID of the player to remove
     */
    public void clearPlayer(UUID uuid, boolean silent) {
        queues.forEach((server, queue) -> queue.removePlayer(uuid, false));

        if(silent) {
            return;
        }

        ProxyQueuesImpl.getInstance().getProxyServer().getPlayer(uuid).ifPresent(
                onlinePlayer -> notifyQueueRemoval(onlinePlayer, "commands.leave-success", MessageType.INFO));
    }

    public void kickPlayer(Player player) {
        clearPlayer(player, true);
        notifyQueueRemoval(player, "errors.queue-removed", MessageType.ERROR);
    }

    public void kickPlayer(UUID uuid) {
        clearPlayer(uuid, true);

        ProxyQueuesImpl.getInstance().getProxyServer().getPlayer(uuid).ifPresent(
                onlinePlayer -> notifyQueueRemoval(onlinePlayer, "errors.queue-removed", MessageType.ERROR));
    }

    private void notifyQueueRemoval(Player player, String message, MessageType messageType) {
        if(!player.isActive()) {
            return;
        }

        RegisteredServer waitingServer = proxyQueues.getWaitingServer().orElse(null);
        Optional<ServerConnection> currentServer = player.getCurrentServer();

        if(currentServer.isPresent() && currentServer.get().getServer().equals(waitingServer)) {
            player.disconnect(Messages.getComponent(message));
        } else {
            proxyQueues.sendMessage(player, messageType, message);
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

                RegisteredServer server = proxyQueues.getProxyServer().getServer(serverName).orElseThrow();

                ProxyQueue queue = createQueue(server, requiredPlayers, maxNormal, maxPriority, maxStaff);
                queue.setDelayLength(settingsManager.getProperty(ConfigOptions.DELAY_LENGTH));

                queuedServers.add(server);
            } catch (Exception ex) {
                proxyQueues.getLogger().warn("It seems like one of your servers was configured invalidly in the config.");
                ex.printStackTrace();
            }
        });

        Iterator<Map.Entry<RegisteredServer, ProxyQueue>> it = queues.entrySet().iterator();

        //Delete queues removed from config
        while(it.hasNext()) {
            Map.Entry<RegisteredServer, ProxyQueue> pair = it.next();

            if(!queuedServers.contains(pair.getKey())) {
                proxyQueues.getLogger().info("Deleting queue for " + pair.getKey().getServerInfo().getName());
                pair.getValue().destroy();
                it.remove();
            }
        }
    }
}
