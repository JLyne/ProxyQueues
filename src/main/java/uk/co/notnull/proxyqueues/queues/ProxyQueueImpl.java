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
import com.velocitypowered.api.plugin.PluginContainer;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.api.scheduler.ScheduledTask;
import uk.co.notnull.proxyqueues.api.MessageType;
import uk.co.notnull.proxyqueues.ProxyQueuesImpl;
import uk.co.notnull.proxyqueues.api.QueueType;
import uk.co.notnull.proxyqueues.api.events.PlayerQueueEvent;
import uk.co.notnull.proxyqueues.api.queues.QueuePlayer;
import uk.co.notnull.proxyqueues.configuration.sections.ConfigOptions;
import uk.co.notnull.proxyqueues.Messages;
import uk.co.notnull.proxyqueues.tasks.QueueMoveTask;

import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;


public final class ProxyQueueImpl implements uk.co.notnull.proxyqueues.api.queues.ProxyQueue {

    private final ProxyQueuesImpl proxyQueues;

    private final ConcurrentLinkedQueue<QueuePlayerImpl> queue = new ConcurrentLinkedQueue<>();
    private final ConcurrentLinkedQueue<QueuePlayerImpl> priorityQueue = new ConcurrentLinkedQueue<>();
    private final ConcurrentLinkedQueue<QueuePlayerImpl> staffQueue = new ConcurrentLinkedQueue<>();

    //Cached queue sizes
    private final AtomicInteger queueSize = new AtomicInteger(0);
    private final AtomicInteger priorityQueueSize = new AtomicInteger(0);
    private final AtomicInteger staffQueueSize = new AtomicInteger(0);

    private final ConcurrentHashMap<UUID, QueuePlayerImpl> queuePlayers = new ConcurrentHashMap<>();

    //Cache of connected priority/staff players, to ease calculation of queue player thresholds
    private final Set<UUID> connectedPriority = ConcurrentHashMap.newKeySet();
    private final Set<UUID> connectedStaff = ConcurrentHashMap.newKeySet();

    private final RegisteredServer server;
    private ScheduledTask scheduledTask;
    private int delayLength;
    private int playersRequired;

    private int maxSlots;
    private int priorityMaxSlots;
    private int staffMaxSlots;

    private final SettingsManager settingsManager;

    private final ProxyQueueEventHandler eventHandler;
    private final ProxyQueueNotifier notifier;
    private final QueueMoveTask moveTask;

    private final Map<PluginContainer, String> pauses = new HashMap<>();

    public ProxyQueueImpl(ProxyQueuesImpl proxyQueues, RegisteredServer server, int playersRequired, int maxSlots, int priorityMaxSlots, int staffMaxSlots) {
        this.proxyQueues = proxyQueues;
        this.server = server;
        this.settingsManager = proxyQueues.getSettingsHandler().getSettingsManager();
        this.delayLength = settingsManager.getProperty(ConfigOptions.DELAY_LENGTH);
        this.playersRequired = Math.max(playersRequired, 0);

        this.maxSlots = Math.max(maxSlots, playersRequired);
        this.priorityMaxSlots = Math.max(priorityMaxSlots, maxSlots);
        this.staffMaxSlots = Math.max(staffMaxSlots, priorityMaxSlots);

        this.notifier = new ProxyQueueNotifier(proxyQueues, this);
        this.eventHandler = new ProxyQueueEventHandler(proxyQueues, this);
        proxyQueues.getProxyServer().getEventManager().register(proxyQueues, eventHandler);

        this.moveTask = new QueueMoveTask(this, server, proxyQueues);
        this.scheduledTask = proxyQueues.getProxyServer().getScheduler().buildTask(proxyQueues, moveTask)
                .repeat(delayLength, TimeUnit.SECONDS).schedule();
    }

    public void addPlayer(Player player) {
        QueueType queueType = QueueType.NORMAL;

         if(player.hasPermission(settingsManager.getProperty(ConfigOptions.PRIORITY_PERMISSION))) {
             queueType = QueueType.PRIORITY;
         } else if(player.hasPermission(settingsManager.getProperty(ConfigOptions.STAFF_PERMISSION))) {
             queueType = QueueType.STAFF;
         }

        addPlayer(player, queueType);
    }

    /**
     * Add a player to a queue.
     * If the player is already in the queue, but disconnected from the proxy, their current queue position and type will be used
     * Otherwise the provided queueType will be used, and the player will be added to the back of the queue
     * @param player the player to add
     */
    public void addPlayer(Player player, QueueType queueType) {
        AtomicBoolean added = new AtomicBoolean(false);

        shouldAddPlayer(player).thenApply(shouldAdd -> {
            if(!shouldAdd) {
                removePlayer(player, false);
                return null;
            }

            return queuePlayers.compute(player.getUniqueId(), (uuid, queuePlayer) -> {
                if(queuePlayer == null) {  //New player
                    added.set(true);
                    return new QueuePlayerImpl(player, this, queueType);
                } else if(!queuePlayer.getPlayer().equals(player)) { //Player was previous in queue before disconnecting, update and reuse object
                    queuePlayer.setPlayer(player);
                    queuePlayer.setLastSeen(Instant.now());
                }

                return queuePlayer;
            });
        }).thenAccept(result -> {
            if(result != null) {
                //Only add to queue if they aren't already there
                if(added.get()) {
                    proxyQueues.getLogger().info("Added " + player.getUsername() + " to queue");
                    switch (result.getQueueType()) {
                        case STAFF -> {
                            staffQueue.add(result);
                            staffQueueSize.incrementAndGet();
                        }
                        case PRIORITY -> {
                            priorityQueue.add(result);
                            priorityQueueSize.incrementAndGet();
                        }
                        default -> {
                            queue.add(result);
                            queueSize.incrementAndGet();
                        }
                    }
                } else {
                    proxyQueues.getLogger().info("Restoring queue position of " + player.getUsername());

                    if(result.getQueueType() == QueueType.PRIORITY) {
                        proxyQueues.sendMessage(result.getPlayer(), MessageType.INFO, "reconnect.restore-priority");
                    } else {
                        proxyQueues.sendMessage(result.getPlayer(), MessageType.INFO, "reconnect.restore-position");
                    }
                }

                if(isPaused()) {
                    notifier.notifyPause(result);
                }
            }
        });
    }

    private CompletableFuture<Boolean> shouldAddPlayer(Player player) {
        PlayerQueueEvent event = new PlayerQueueEvent(player, server);
        return proxyQueues.getProxyServer().getEventManager().fire(event).thenApply((PlayerQueueEvent result) -> {
            //Don't add to queue if event cancelled, show player the reason
            if (result.isCancelled()) {
                String reason = result.getReason() != null ? result.getReason() : "An unexpected error occurred. Please try again later";
                ServerConnection currentServer = player.getCurrentServer().orElse(null);
                RegisteredServer waitingServer = proxyQueues.getWaitingServer().orElse(null);

                proxyQueues.getLogger().info(player.getUsername() + "'s PlayerQueueEvent cancelled");

                if (currentServer == null || currentServer.getServer().equals(waitingServer)) {
                    player.disconnect(
                            Messages.getComponent("errors.queue-cannot-join",
                                                  Collections.singletonMap("reason", reason),
                                                  Collections.emptyMap()));
                } else {
                    proxyQueues.sendMessage(player, MessageType.ERROR, "errors.queue-cannot-join",
                                            Collections.singletonMap("reason", reason));
                }
            }

            return !result.isCancelled();
        });
    }

    /**
     * Removes the player from the queue, and updates connected caches
     * @param player - The player
     * @param connected - Whether player has now connected to the queued server, for cache updates
     */
    public void removePlayer(QueuePlayer player, boolean connected) {
        boolean removed;

        switch (player.getQueueType()) {
            case STAFF -> {
                removed = staffQueue.remove((QueuePlayerImpl) player);

                //Update connected players cache
                if (connected) {
                    connectedStaff.add(
                            player.getPlayer().getUniqueId()); //Is connected to queued server, add to connected
                }
                if (removed) {
                    staffQueueSize.decrementAndGet();
                }
            }
            case PRIORITY -> {
                removed = priorityQueue.remove((QueuePlayerImpl) player);

                //Update connected players cache
                if (connected) {
                    connectedPriority.add(
                            player.getPlayer().getUniqueId()); //Is connected to queued server, add to connected
                }
                if (removed) {
                    priorityQueueSize.decrementAndGet();
                }
            }
            default -> {
                removed = queue.remove((QueuePlayerImpl) player);
                if (removed) {
                    queueSize.decrementAndGet();
                }
            }
        }

        if(!connected) {
            clearConnectedState(player.getPlayer());
        }

        player.hideBossBar();
        ((QueuePlayerImpl) player).setConnecting(false);

        proxyQueues.getLogger().debug("removePlayer: type = " + player.getQueueType() + ", removed? " + removed);
        queuePlayers.remove(player.getPlayer().getUniqueId());
    }

    /**
     * Removes the player from the queue, and updates connected caches
     * @param player - The player
     * @param connected - Whether player has now connected to the queued server, for cache updates
     */
    public void removePlayer(Player player, boolean connected) {
        Optional<QueuePlayer> queuePlayer = getQueuePlayer(player, false);

        if(queuePlayer.isPresent()) {
            removePlayer(queuePlayer.get(), connected);
        } else {
            proxyQueues.getLogger().debug("Not in queue, removing cached entries");
            clearConnectedState(player);
        }
    }

    /**
     * Removes the player from the queue, and updates connected caches
     * @param uuid - The uuid of the player
     * @param connected - Whether player has now connected to the queued server, for cache updates
     */
    public void removePlayer(UUID uuid, boolean connected) {
        Optional<QueuePlayer> queuePlayer = getQueuePlayer(uuid);

        if(queuePlayer.isPresent()) {
            removePlayer(queuePlayer.get(), connected);
        } else {
            proxyQueues.getLogger().debug("Not in queue, removing cached entries");
            clearConnectedState(uuid);
        }
    }

    public void destroy() {
        proxyQueues.getProxyServer().getEventManager().unregisterListener(proxyQueues, eventHandler);
        scheduledTask.cancel();

        clearQueue(staffQueue, true);
        clearQueue(priorityQueue, true);
        clearQueue(queue, true);

        queuePlayers.clear();
    }

    private void clearQueue(ConcurrentLinkedQueue<QueuePlayerImpl> q, boolean destroying) {
        Optional<RegisteredServer> waitingServer = proxyQueues.getWaitingServer();

        for (QueuePlayerImpl player : q) {
            removePlayer(player, false);

            Optional<ServerConnection> currentServer = player.getPlayer().getCurrentServer();

            if (waitingServer.isEmpty() || currentServer.isEmpty() || waitingServer.get().equals(
                    currentServer.get().getServer())) {
                if (destroying) {
                    player.getPlayer().disconnect(
                            Messages.getComponent("errors.queue-destroyed",
                                                  Collections.singletonMap("server", server.getServerInfo().getName()),
                                                  Collections.emptyMap()));
                } else {
                    player.getPlayer().disconnect(Messages.getComponent("errors.queue-removed"));
                }
            } else {
                if (destroying) {
                    proxyQueues.sendMessage(player.getPlayer(), MessageType.ERROR, "errors.queue-destroyed",
                                            Collections.singletonMap("server", server.getServerInfo().getName()));
                } else {
                    proxyQueues.sendMessage(player.getPlayer(), MessageType.ERROR, "errors.queue-removed");
                }
            }
        }
    }

    public void clear() {
        //FIXME: Players could join concurrently while the queue is being cleared. Not a big deal I guess?
        clearQueue(staffQueue, false);
        clearQueue(priorityQueue, false);
        clearQueue(queue, false);
        queuePlayers.clear();
    }

    /**
     * Returns whether the player is in this queue
     * @param player - The player
     * @return Whether the player is queued
     */
    public boolean isPlayerQueued(Player player) {
        return getQueuePlayer(player, false).isPresent();
    }

    public boolean isPlayerQueued(UUID uuid) {
        return getQueuePlayer(uuid).isPresent();
    }

    public Optional<QueuePlayer> getQueuePlayer(Player player, boolean strict) {
        QueuePlayerImpl queuePlayer = queuePlayers.get(player.getUniqueId());

        if(queuePlayer == null) {
            queuePlayer = queuePlayers.computeIfAbsent(player.getUniqueId(), key -> null);
        }

        if(strict && queuePlayer != null && !queuePlayer.getPlayer().equals(player)) {
            queuePlayer = null;
        }

        return Optional.ofNullable(queuePlayer);
    }

    public Optional<QueuePlayer> getQueuePlayer(UUID uuid) {
        QueuePlayerImpl queuePlayer = queuePlayers.get(uuid);

        if(queuePlayer == null) {
            queuePlayer = queuePlayers.computeIfAbsent(uuid, key -> null);
        }

        return Optional.ofNullable(queuePlayer);
    }

    /**
     * Returns whether the queue is active, and that players trying to join should be added to it
     * @return added or not
     */
    public boolean isActive() {
        return server.getPlayersConnected().size() >= playersRequired;
    }

    public boolean isServerFull(QueueType queueType) {
        int modSlots = getMaxSlots(QueueType.STAFF) - getMaxSlots(QueueType.PRIORITY);
        int prioritySlots = getMaxSlots(QueueType.PRIORITY) - getMaxSlots(QueueType.NORMAL);

        int usedModSlots;
        int usedPrioritySlots;
        int totalPlayers = server.getPlayersConnected().size();

        switch(queueType) {
            //Staff, check total count is below staff limit
            case STAFF:
                break;

            //Priority, check total count ignoring filled mod slots is below priority limit
            case PRIORITY:
                usedModSlots = Math.min(connectedStaff.size(), modSlots); //Ignore staff beyond assigned slots, prevents mods "stealing" normal slots if normal players leave
                totalPlayers -= usedModSlots;

                break;

            //Normal, check total count ignoring filled mod and priority slots is below normal limit
            case NORMAL:
            default:
                usedModSlots = Math.min(connectedStaff.size(), modSlots); //Ignore staff beyond assigned slots, prevents mods "stealing" normal slots if normal players leave
                usedPrioritySlots = Math.min(connectedPriority.size() + (connectedStaff.size() - usedModSlots), prioritySlots); //Count mods not counted above as filled priority slots, prevents mods "stealing" normal slots if normal players leave
                totalPlayers -= (usedModSlots + usedPrioritySlots);

                break;
        }

        return totalPlayers >= getMaxSlots(queueType);
    }

    public int getMaxSlots(QueueType queueType) {
        return switch (queueType) {
            case STAFF -> staffMaxSlots;
            case PRIORITY -> priorityMaxSlots;
            default -> maxSlots;
        };
    }

    public int getQueueSize(QueueType queueType) {
        return switch (queueType) {
            case STAFF -> staffQueueSize.get();
            case PRIORITY -> priorityQueueSize.get();
            default -> queueSize.get();
        };
    }

    public int getConnectedCount() {
        return server.getPlayersConnected().size();
    }

    public int getConnectedCount(QueueType queueType) {
        return switch (queueType) {
            case STAFF -> connectedStaff.size();
            case PRIORITY -> connectedPriority.size();
            default -> server.getPlayersConnected().size() - connectedStaff.size() - connectedPriority.size();
        };
    }

    public QueuePlayer[] getTopPlayers(QueueType queueType, int count) {
        QueuePlayer[] players = new QueuePlayer[count];
        ConcurrentLinkedQueue<QueuePlayerImpl> queue = switch (queueType) {
            case STAFF -> staffQueue;
            case PRIORITY -> priorityQueue;
            default -> this.queue;
        };

        int index = 0;

        for(QueuePlayerImpl player : queue) {
            players[index] = player;

            if(++index > 2) {
                break;
            }
        }

        return players;
    }

    public ConcurrentLinkedQueue<QueuePlayerImpl> getQueue() {
        return queue;
    }

    public ConcurrentLinkedQueue<QueuePlayerImpl> getPriorityQueue() {
        return priorityQueue;
    }

    public ConcurrentLinkedQueue<QueuePlayerImpl> getStaffQueue() {
        return staffQueue;
    }

    public RegisteredServer getServer() {
        return this.server;
    }

    public int getPlayersRequired() {
        return this.playersRequired;
    }

    public ProxyQueueNotifier getNotifier() {
        return notifier;
    }

    public void setDelayLength(int delayLength) {
        if(delayLength != this.delayLength) {
            scheduledTask.cancel();
            scheduledTask = proxyQueues.getProxyServer().getScheduler().buildTask(proxyQueues, moveTask)
                .repeat(delayLength, TimeUnit.SECONDS).schedule();
        }

        this.delayLength = delayLength;
    }

    public void setPlayersRequired(int playersRequired) {
        this.playersRequired = playersRequired;
    }

    public void setMaxSlots(int maxSlots) {
        this.maxSlots = maxSlots;
    }

    public void setPriorityMaxSlots(int priorityMaxSlots) {
        this.priorityMaxSlots = priorityMaxSlots;
    }

    public void setStaffMaxSlots(int staffMaxSlots) {
        this.staffMaxSlots = staffMaxSlots;
    }

    public String toString() {
        return "ProxyQueue(queue=" + this.getQueue() + ", server=" + this.getServer() + ", delayLength=" + this.delayLength + ", playersRequired=" + this.getPlayersRequired() + ", maxSlots=" + this.getMaxSlots(QueueType.STAFF) + ", notifyMethod=" + this.getNotifier().getNotifyMethod() + ")";
    }

    void clearConnectedState(Player player) {
        connectedStaff.remove(player.getUniqueId());
        connectedPriority.remove(player.getUniqueId());
    }

    void clearConnectedState(UUID uuid) {
        connectedStaff.remove(uuid);
        connectedPriority.remove(uuid);
    }

    public void addPause(Object plugin, String reason) {
        Optional<PluginContainer> container = this.proxyQueues.getProxyServer().getPluginManager().fromInstance(plugin);

        if(container.isEmpty()) {
            throw new IllegalArgumentException("plugin is not registered");
        }

        pauses.put(container.get(), reason);

        if(pauses.size() == 1) {
            notifier.notifyPause();
        }
    }

    public void removePause(Object plugin) {
       Optional<PluginContainer> container = this.proxyQueues.getProxyServer().getPluginManager().fromInstance(plugin);

        if(container.isEmpty()) {
            throw new IllegalArgumentException("plugin is not registered");
        }

        if(pauses.remove(container.get()) != null && pauses.isEmpty()) {
            notifier.notifyResume();
        }
    }

    public boolean hasPause(Object plugin) {
        Optional<PluginContainer> container = this.proxyQueues.getProxyServer().getPluginManager().fromInstance(plugin);

        if(container.isEmpty()) {
            throw new IllegalArgumentException("plugin is not registered");
        }

        return pauses.containsKey(container.get());
    }

    public boolean isPaused() {
        return !pauses.isEmpty();
    }

    public Map<PluginContainer, String> getPauses() {
        return Collections.unmodifiableMap(pauses);
    }
}
