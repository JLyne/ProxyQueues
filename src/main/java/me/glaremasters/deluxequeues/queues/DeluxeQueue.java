package me.glaremasters.deluxequeues.queues;

import ch.jalu.configme.SettingsManager;
import co.aikar.commands.MessageType;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.api.scheduler.ScheduledTask;
import me.glaremasters.deluxequeues.DeluxeQueues;
import me.glaremasters.deluxequeues.QueueType;
import me.glaremasters.deluxequeues.configuration.sections.ConfigOptions;
import me.glaremasters.deluxequeues.events.PlayerQueueEvent;
import me.glaremasters.deluxequeues.messages.Messages;
import me.glaremasters.deluxequeues.tasks.QueueMoveTask;
import net.kyori.text.TextComponent;
import net.kyori.text.format.TextColor;

import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;

/**
 * Created by Glare
 * Date: 7/13/2019
 * Time: 10:30 PM
 */
public class DeluxeQueue {

    private final DeluxeQueues deluxeQueues;

    private final ConcurrentLinkedQueue<QueuePlayer> queue = new ConcurrentLinkedQueue<>();
    private final ConcurrentLinkedQueue<QueuePlayer> priorityQueue = new ConcurrentLinkedQueue<>();
    private final ConcurrentLinkedQueue<QueuePlayer> staffQueue = new ConcurrentLinkedQueue<>();

    private final ConcurrentHashMap<UUID, QueuePlayer> queuePlayers = new ConcurrentHashMap<>();

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

    private final DeluxeQueueEventHandler eventHandler;
    private final DeluxeQueueNotifier notifier;
    private final QueueMoveTask moveTask;

    public DeluxeQueue(DeluxeQueues deluxeQueues, RegisteredServer server, int playersRequired, int maxSlots, int priorityMaxSlots, int staffMaxSlots) {
        this.deluxeQueues = deluxeQueues;
        this.server = server;
        this.settingsManager = deluxeQueues.getSettingsHandler().getSettingsManager();
        this.delayLength = settingsManager.getProperty(ConfigOptions.DELAY_LENGTH);
        this.playersRequired = Math.max(playersRequired, 0);

        this.maxSlots = Math.max(maxSlots, playersRequired);
        this.priorityMaxSlots = Math.max(priorityMaxSlots, maxSlots);
        this.staffMaxSlots = Math.max(staffMaxSlots, priorityMaxSlots);

        this.notifier = new DeluxeQueueNotifier(deluxeQueues, this);
        this.eventHandler = new DeluxeQueueEventHandler(deluxeQueues, this);
        deluxeQueues.getProxyServer().getEventManager().register(deluxeQueues, eventHandler);

        this.moveTask = new QueueMoveTask(this, server, deluxeQueues);
        this.scheduledTask = deluxeQueues.getProxyServer().getScheduler().buildTask(deluxeQueues, moveTask)
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
        Optional<QueuePlayer> qp = getQueuePlayer(player, false);
        QueuePlayer queuePlayer;
        boolean restored;

        if(qp.isPresent() && qp.get().getPlayer().equals(player)) { //Player is already in queue
            return;
        } else if(qp.isPresent()) { //Player was previous in queue before disconnecting, update and reuse object
            deluxeQueues.getLogger().info(player.getUsername() + " is already in queue. Restoring position");

            queuePlayer = qp.get();
            queuePlayer.setPlayer(player);
            queuePlayer.setLastSeen(Instant.now());
            restored = true;
        } else { //New player
             queuePlayer = new QueuePlayer(player, queueType);
             restored = false;
        }

        queuePlayers.putIfAbsent(player.getUniqueId(), queuePlayer);

        PlayerQueueEvent event = new PlayerQueueEvent(player, server);

        deluxeQueues.getProxyServer().getEventManager().fire(event).thenAcceptAsync(result -> {
            //Don't add to queue if event cancelled, show player the reason
            if (result.isCancelled()) {
                deluxeQueues.getLogger().info(player.getUsername() + "'s PlayerQueueEvent cancelled. Removing from queue.");
                deluxeQueues.getCommandManager().sendMessage(player, MessageType.ERROR,
                                                             Messages.ERRORS__QUEUE_CANNOT_JOIN);
                player.sendMessage(TextComponent.of(result.getReason()).color(TextColor.RED));

                removePlayer(queuePlayer, false);

                return;
            }

            //Only add to queue if they aren't already there
            if(!restored) {
                switch(queuePlayer.getQueueType()) {
                    case STAFF:
                        staffQueue.add(queuePlayer);
                        break;

                    case PRIORITY:
                        priorityQueue.add(queuePlayer);
                        break;

                    case NORMAL:
                    default:
                        queue.add(queuePlayer);
                        break;
                }
            } else {
                deluxeQueues.getCommandManager().getCommandIssuer(queuePlayer.getPlayer())
                        .sendInfo(Messages.RECONNECT__RESTORE_SUCCESS);
            }
        });
    }

    /**
     * Removes the player from the queue, and updates connected caches
     * @param player - The player
     * @param connected - Whether player has now connected to the queued server, for cache updates
     */
    public void removePlayer(QueuePlayer player, boolean connected) {
        player.getBossBar().removeAllPlayers();
        player.setConnecting(false);
        boolean removed;

        if(!connected) {
            deluxeQueues.getLogger().info("Not connected to queued server, removing cache entry");
            clearConnectedState(player.getPlayer());
        }

        switch(player.getQueueType()) {
            case STAFF:
                removed = staffQueue.remove(player);

                //Update connected players cache
                if(connected) {
                    deluxeQueues.getLogger().info("Connected to queued server, adding cache entry");
                    connectedStaff.add(player.getPlayer().getUniqueId()); //Is connected to queued server, add to connected
                }

                break;

            case PRIORITY:
                removed = priorityQueue.remove(player);

                //Update connected players cache
                if(connected) {
                    deluxeQueues.getLogger().info("Connected to queued server, adding cache entry");
                    connectedPriority.add(player.getPlayer().getUniqueId()); //Is connected to queued server, add to connected
                }

                break;

            case NORMAL:
            default:
                removed = queue.remove(player);
                break;
        }

        deluxeQueues.getLogger().info("removePlayer: type = " + player.getQueueType() + ", removed? " + removed);
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
            deluxeQueues.getLogger().info("Not in queue, removing cached entries");
            clearConnectedState(player);
        }
    }

    public void destroy() {
        deluxeQueues.getProxyServer().getEventManager().unregisterListener(deluxeQueues, eventHandler);
        scheduledTask.cancel();

        clearQueue(staffQueue);
        clearQueue(priorityQueue);
        clearQueue(queue);

        queuePlayers.clear();
    }

    private void clearQueue(ConcurrentLinkedQueue<QueuePlayer> q) {
        Optional<RegisteredServer> waitingServer = deluxeQueues.getWaitingServer();

        for (QueuePlayer player : q) {
            removePlayer(player, false);

            Optional<ServerConnection> currentServer = player.getPlayer().getCurrentServer();

            if(!waitingServer.isPresent() || !currentServer.isPresent() || waitingServer.get().equals(currentServer.get().getServer())) {
                player.getPlayer().disconnect(TextComponent.of(deluxeQueues.getCommandManager()
                                      .formatMessage(deluxeQueues.getCommandManager().getCommandIssuer(player.getPlayer()),
                                                     MessageType.ERROR,
                                                     Messages.ERRORS__QUEUE_DESTROYED, "%server%", server.getServerInfo().getName())));
            } else {
                deluxeQueues.getCommandManager().getCommandIssuer(player.getPlayer())
                        .sendError(Messages.ERRORS__QUEUE_DESTROYED, "%server%", server.getServerInfo().getName());
            }
        }
    }

    /**
     * Returns whether the player is in this queue
     * @param player - The player
     * @return Whether the player is queued
     */
    public boolean isPlayerQueued(Player player) {
        return getQueuePlayer(player, false).isPresent();
    }

    public Optional<QueuePlayer> getQueuePlayer(Player player, boolean strict) {
        QueuePlayer queuePlayer = queuePlayers.get(player.getUniqueId());

        if(queuePlayer == null) {
            queuePlayer = queuePlayers.computeIfAbsent(player.getUniqueId(), key -> null);
        }

        if(strict && queuePlayer != null && !queuePlayer.getPlayer().equals(player)) {
            queuePlayer = null;
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
        switch (queueType) {
            case STAFF:
                return staffMaxSlots;
            case PRIORITY:
                return priorityMaxSlots;
            case NORMAL:
            default:
                return maxSlots;
        }
    }

    public int getQueueSize(QueueType queueType) {
        switch (queueType) {
            case STAFF:
                return staffQueue.size();
            case PRIORITY:
                return priorityQueue.size();
            case NORMAL:
            default:
                return queue.size();
        }
    }

    public int getConnectedCount() {
        return server.getPlayersConnected().size();
    }

    public int getConnectedCount(QueueType queueType) {
        switch (queueType) {
            case STAFF:
                return connectedStaff.size();
            case PRIORITY:
                return connectedPriority.size();
            case NORMAL:
            default:
                return server.getPlayersConnected().size() - connectedStaff.size() - connectedPriority.size();
        }
    }

    public QueuePlayer[] getTopPlayers(QueueType queueType, int count) {
        QueuePlayer[] players = new QueuePlayer[count];
        ConcurrentLinkedQueue<QueuePlayer> queue;

        switch (queueType) {
            case STAFF:
                queue = staffQueue;
                break;
            case PRIORITY:
                queue = priorityQueue;
                break;
            case NORMAL:
            default:
                queue = this.queue;
                break;
        }

        int index = 0;

        for(QueuePlayer player : queue) {
            players[index] = player;

            if(++index > 2) {
                break;
            }
        }

        return players;
    }

    public ConcurrentLinkedQueue<QueuePlayer> getQueue() {
        return queue;
    }

    public ConcurrentLinkedQueue<QueuePlayer> getPriorityQueue() {
        return priorityQueue;
    }

    public ConcurrentLinkedQueue<QueuePlayer> getStaffQueue() {
        return staffQueue;
    }

    public RegisteredServer getServer() {
        return this.server;
    }

    public int getPlayersRequired() {
        return this.playersRequired;
    }

    public DeluxeQueueNotifier getNotifier() {
        return notifier;
    }

    public void setDelayLength(int delayLength) {
        if(delayLength != this.delayLength) {
            scheduledTask.cancel();
            scheduledTask = deluxeQueues.getProxyServer().getScheduler().buildTask(deluxeQueues, moveTask)
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
        return "DeluxeQueue(queue=" + this.getQueue() + ", server=" + this.getServer() + ", delayLength=" + this.delayLength + ", playersRequired=" + this.getPlayersRequired() + ", maxSlots=" + this.getMaxSlots(QueueType.STAFF) + ", notifyMethod=" + this.getNotifier().getNotifyMethod() + ")";
    }

    void clearConnectedState(Player player) {
        connectedStaff.remove(player.getUniqueId());
        connectedPriority.remove(player.getUniqueId());
    }
}
