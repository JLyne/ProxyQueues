package me.glaremasters.deluxequeues.queues;

import ch.jalu.configme.SettingsManager;
import co.aikar.commands.ACFVelocityUtil;
import co.aikar.commands.MessageType;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.api.util.MessagePosition;
import com.velocitypowered.api.util.title.TextTitle;
import me.glaremasters.deluxequeues.DeluxeQueues;
import me.glaremasters.deluxequeues.QueueType;
import me.glaremasters.deluxequeues.configuration.sections.ConfigOptions;
import me.glaremasters.deluxequeues.events.PlayerQueueEvent;
import me.glaremasters.deluxequeues.messages.Messages;
import me.glaremasters.deluxequeues.tasks.QueueMoveTask;
import net.kyori.text.TextComponent;
import net.kyori.text.format.TextColor;

import java.util.Optional;
import java.util.Set;
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

    private final ConcurrentHashMap<Player, QueuePlayer> queuePlayers = new ConcurrentHashMap<>();

    //Cache of connected priority/staff players, to ease calculation of queue player thresholds
    private final Set<Player> connectedPriority = ConcurrentHashMap.newKeySet();
    private final Set<Player> connectedStaff = ConcurrentHashMap.newKeySet();

    private final RegisteredServer server;
    private final int delayLength;
    private final int playersRequired;

    private final int maxSlots;
    private final int priorityMaxSlots;
    private final int staffMaxSlots;

    private final SettingsManager settingsManager;
    private final String notifyMethod;

    public DeluxeQueue(DeluxeQueues deluxeQueues, RegisteredServer server, int playersRequired, int maxSlots, int priorityMaxSlots, int staffMaxSlots) {
        this.deluxeQueues = deluxeQueues;
        this.server = server;
        this.settingsManager = deluxeQueues.getSettingsHandler().getSettingsManager();
        this.delayLength = settingsManager.getProperty(ConfigOptions.DELAY_LENGTH);
        this.playersRequired = Math.max(playersRequired, 0);

        this.maxSlots = Math.max(maxSlots, playersRequired);
        this.priorityMaxSlots = Math.max(priorityMaxSlots, maxSlots);
        this.staffMaxSlots = Math.max(staffMaxSlots, priorityMaxSlots);

        this.notifyMethod = settingsManager.getProperty(ConfigOptions.INFORM_METHOD);

        deluxeQueues.getProxyServer().getScheduler().buildTask(deluxeQueues, new QueueMoveTask(this, server, deluxeQueues))
                .repeat(delayLength, TimeUnit.SECONDS).schedule();
    }

    public void addPlayer(Player player) {
        addPlayer(player, null);
    }

    /**
     * Add a player to a queue
     * @param player the player to add
     */
    public void addPlayer(Player player, QueueType queueType) {
        Optional<QueuePlayer> qp = getQueuePlayer(player);

        if(qp.isPresent()) {
            return;
        }

        if(queueType == null) {
            queueType = QueueType.NORMAL;

            if(player.hasPermission(settingsManager.getProperty(ConfigOptions.PRIORITY_PERMISSION))) {
                queueType = QueueType.PRIORITY;
            } else if(player.hasPermission(settingsManager.getProperty(ConfigOptions.STAFF_PERMISSION))) {
                queueType = QueueType.STAFF;
            }
        }

        QueuePlayer queuePlayer = new QueuePlayer(player, queueType);
        queuePlayers.put(player, queuePlayer);

        deluxeQueues.getProxyServer().getEventManager().fire(new PlayerQueueEvent(player, server))
                .thenAcceptAsync(result -> {
                    //Don't add to queue if event cancelled, show player the reason
                    if (result.isCancelled()) {
                        deluxeQueues.getCommandManager().sendMessage(player, MessageType.ERROR,
                                                                     Messages.ERRORS__QUEUE_CANNOT_JOIN);
                        player.sendMessage(TextComponent.of(result.getReason()).color(TextColor.RED));
                        queuePlayers.remove(player, queuePlayer);
                        return;
                    }

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
                });
    }

    public void removePlayer(QueuePlayer player, boolean connected) {
        player.getBossBar().removeAllPlayers();
        player.setConnecting(false);

        switch(player.getQueueType()) {
            case STAFF:
                staffQueue.remove(player);

                //Update connected players cache
                if(connected) {
                    deluxeQueues.getLogger().info("Connected to queued server, adding cache entry");
                    connectedStaff.add(player.getPlayer()); //Is connected to queued server, add to connected
                } else {
                    deluxeQueues.getLogger().info("Not connected to queued server, removing cache entry");
                    connectedStaff.remove(player.getPlayer()); //Is not connected to queued server, remove from connected
                }

                break;

            case PRIORITY:
                priorityQueue.remove(player);

                //Update connected players cache
                if(connected) {
                    deluxeQueues.getLogger().info("Connected to queued server, adding cache entry");
                    connectedPriority.add(player.getPlayer()); //Is connected to queued server, add to connected
                } else {
                    deluxeQueues.getLogger().info("Not connected to queued server, removing cache entry");
                    connectedPriority.remove(player.getPlayer()); //Is not connected to queued server, remove from connected
                }

                break;

            case NORMAL:
            default:
                queue.remove(player);
                break;
        }

        queuePlayers.remove(player.getPlayer());
    }

    public void removePlayer(Player player, boolean connected) {
        Optional<QueuePlayer> queuePlayer = getQueuePlayer(player);

        if(queuePlayer.isPresent()) {
            removePlayer(queuePlayer.get(), connected);
        } else {
            deluxeQueues.getLogger().info("Not in queue, removing cached entries");

            connectedStaff.remove(player);
            connectedPriority.remove(player);
        }
    }

    public Optional<QueuePlayer> getQueuePlayer(Player player) {
        QueuePlayer queuePlayer = queuePlayers.get(player);

        if(queuePlayer == null) {
            return Optional.ofNullable(queuePlayers.computeIfAbsent(player, key -> null));
        } else {
            return Optional.of(queuePlayer);
        }
    }

    /**
     * Whether the queue is active, and that players trying to join should be added to it
     * @return added or not
     */
    public boolean isActive() {
        return server.getPlayersConnected().size() >= playersRequired;
    }

    public boolean isPlayerQueued(Player player) {
        return getQueuePlayer(player).isPresent();
    }

    public boolean isServerFull(QueueType queueType) {
        int modSlots;
        int usedModSlots;

        switch(queueType) {
            //Staff, check total count is below staff limit
            case STAFF:
                deluxeQueues.getLogger().info("STAFF: " + server.getPlayersConnected().size() + " >= " + getMaxSlots(QueueType.STAFF) + "?");

                return server.getPlayersConnected().size() >= getMaxSlots(QueueType.STAFF);

            //Priority, check total count ignoring filled mod slots is below priority limit
            case PRIORITY:
                modSlots = getMaxSlots(QueueType.STAFF) - getMaxSlots(QueueType.PRIORITY);
                usedModSlots = Math.min(connectedStaff.size(), modSlots); //Ignore staff beyond assigned slots, prevents mods "stealing" normal slots if normal players leave

                deluxeQueues.getLogger().info("PRIORITY: " + server.getPlayersConnected().size() + " - " + usedModSlots + " >= " + getMaxSlots(QueueType.PRIORITY) + "?");

                return (server.getPlayersConnected().size() - usedModSlots) >= getMaxSlots(QueueType.PRIORITY);

            //Normal, check total count ignoring filled mod and priority slots is below normal limit
            case NORMAL:
            default:
                modSlots = getMaxSlots(QueueType.STAFF) - getMaxSlots(QueueType.PRIORITY);
                usedModSlots = Math.min(connectedStaff.size(), modSlots); //Ignore staff beyond assigned slots, prevents mods "stealing" normal slots if normal players leave

                int prioritySlots = getMaxSlots(QueueType.PRIORITY) - getMaxSlots(QueueType.NORMAL);
                //Count mods not counted above as filled priority slots, prevents mods "stealing" normal slots if normal players leave
                int usedPrioritySlots = Math.min(connectedPriority.size() + (connectedStaff.size() - usedModSlots), prioritySlots);

                deluxeQueues.getLogger().info("NORMAL: " + server.getPlayersConnected().size() + " - " + usedModSlots + " - " + usedPrioritySlots + " >= " + getMaxSlots(QueueType.NORMAL) + "?");

                return (server.getPlayersConnected().size() - usedModSlots - usedPrioritySlots) >= getMaxSlots(QueueType.NORMAL);

        }
    }

    /**
     * Notify the player that they are in the queue
     * @param player the player to check
     */
    public void notifyPlayer(QueuePlayer player) {
        String actionbar;
        String message;
        String title_top;
        String title_bottom;

        switch(player.getQueueType()) {
            case STAFF:
                actionbar = settingsManager.getProperty(ConfigOptions.STAFF_ACTIONBAR_DESIGN);
                message = settingsManager.getProperty(ConfigOptions.STAFF_TEXT_DESIGN);
                title_top = settingsManager.getProperty(ConfigOptions.STAFF_TITLE_HEADER);
                title_bottom = settingsManager.getProperty(ConfigOptions.STAFF_TITLE_FOOTER);
                break;

            case PRIORITY:
                actionbar = settingsManager.getProperty(ConfigOptions.PRIORITY_ACTIONBAR_DESIGN);
                message = settingsManager.getProperty(ConfigOptions.PRIORITY_TEXT_DESIGN);
                title_top = settingsManager.getProperty(ConfigOptions.PRIORITY_TITLE_HEADER);
                title_bottom = settingsManager.getProperty(ConfigOptions.PRIORITY_TITLE_FOOTER);
                break;

            case NORMAL:
            default:
                actionbar = settingsManager.getProperty(ConfigOptions.NORMAL_ACTIONBAR_DESIGN);
                message = settingsManager.getProperty(ConfigOptions.NORMAL_TEXT_DESIGN);
                title_top = settingsManager.getProperty(ConfigOptions.NORMAL_TITLE_HEADER);
                title_bottom = settingsManager.getProperty(ConfigOptions.NORMAL_TITLE_FOOTER);
                break;
        }

        switch (notifyMethod.toLowerCase()) {
            case "bossbar":
                updateBossBar(player);
                break;
            case "actionbar":
                actionbar = actionbar.replace("{server}", server.getServerInfo().getName());
                actionbar = actionbar.replace("{pos}", String.valueOf(player.getPosition()));
                player.getPlayer().sendMessage(ACFVelocityUtil.color(actionbar), MessagePosition.ACTION_BAR);
                break;
            case "text":
                message = message.replace("{server}", server.getServerInfo().getName());
                message = message.replace("{pos}", String.valueOf(player.getPosition()));
                player.getPlayer().sendMessage(ACFVelocityUtil.color(message), MessagePosition.SYSTEM);
                break;
            case "title":
                TextTitle.Builder title = TextTitle.builder();
                title.title(ACFVelocityUtil.color(title_top));
                title_bottom = title_bottom.replace("{server}", server.getServerInfo().getName());
                title_bottom = title_bottom.replace("{pos}", String.valueOf(player.getPosition()));
                title.subtitle(ACFVelocityUtil.color(title_bottom));
                player.getPlayer().sendTitle(title.build());
                break;
        }
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

    public int getDelayLength() {
        return this.delayLength;
    }

    public int getPlayersRequired() {
        return this.playersRequired;
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

    public String getNotifyMethod() {
        return this.notifyMethod;
    }

    public String toString() {
        return "DeluxeQueue(queue=" + this.getQueue() + ", server=" + this.getServer() + ", delayLength=" + this.getDelayLength() + ", playersRequired=" + this.getPlayersRequired() + ", maxSlots=" + this.getMaxSlots(QueueType.STAFF) + ", notifyMethod=" + this.getNotifyMethod() + ")";
    }

    private void updateBossBar(QueuePlayer player) {
        int position = player.getPosition();
        String message;

        switch (player.getQueueType()) {
            case STAFF:
                message = settingsManager.getProperty(ConfigOptions.STAFF_BOSSBAR_DESIGN);
                break;
            case PRIORITY:
                message = settingsManager.getProperty(ConfigOptions.PRIORITY_BOSSBAR_DESIGN);
                break;
            case NORMAL:
            default:
                message = settingsManager.getProperty(ConfigOptions.NORMAL_BOSSBAR_DESIGN);
                break;
        }

        message = message.replace("{server}", server.getServerInfo().getName());
        message = message.replace("{pos}", String.valueOf(position));

        player.getBossBar().setVisible(true);
        player.getBossBar().setTitle(TextComponent.of(message));
    }
}
