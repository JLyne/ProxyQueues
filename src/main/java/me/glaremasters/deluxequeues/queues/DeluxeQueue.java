package me.glaremasters.deluxequeues.queues;

import ch.jalu.configme.SettingsManager;
import co.aikar.commands.ACFVelocityUtil;
import co.aikar.commands.MessageType;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.api.util.MessagePosition;
import com.velocitypowered.api.util.bossbar.BossBar;
import com.velocitypowered.api.util.bossbar.BossBarColor;
import com.velocitypowered.api.util.bossbar.BossBarOverlay;
import com.velocitypowered.api.util.title.TextTitle;
import me.glaremasters.deluxequeues.DeluxeQueues;
import me.glaremasters.deluxequeues.configuration.sections.ConfigOptions;
import me.glaremasters.deluxequeues.events.PlayerQueueEvent;
import me.glaremasters.deluxequeues.messages.Messages;
import me.glaremasters.deluxequeues.tasks.QueueMoveTask;
import net.kyori.text.TextComponent;
import net.kyori.text.format.TextColor;
import org.checkerframework.checker.units.qual.A;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Created by Glare
 * Date: 7/13/2019
 * Time: 10:30 PM
 */
public class DeluxeQueue {

    private final DeluxeQueues deluxeQueues;
    private final LinkedList<QueuePlayer> queue = new LinkedList<>();
    private final RegisteredServer server;
    private final int delayLength;
    private final int playersRequired;
    private final int maxSlots;
    private final SettingsManager settingsManager;
    private final String notifyMethod;
    private ConcurrentHashMap<UUID, BossBar> bossBars;

    public DeluxeQueue(DeluxeQueues deluxeQueues, RegisteredServer server, int playersRequired, int maxSlots) {
        this.deluxeQueues = deluxeQueues;
        this.server = server;
        this.settingsManager = deluxeQueues.getSettingsHandler().getSettingsManager();
        this.delayLength = settingsManager.getProperty(ConfigOptions.DELAY_LENGTH);
        this.playersRequired = playersRequired;
        this.maxSlots = maxSlots;
        this.notifyMethod = settingsManager.getProperty(ConfigOptions.INFORM_METHOD);
        this.bossBars = new ConcurrentHashMap<>();

        deluxeQueues.getProxyServer().getScheduler().buildTask(deluxeQueues, new QueueMoveTask(this, server))
                .repeat(delayLength, TimeUnit.SECONDS).schedule();
    }

    /**
     * Add a player to a queue
     * @param player the player to add
     */
    public void addPlayer(Player player) {
        if (getFromProxy(player) == null) {
            QueuePlayer qp = new QueuePlayer(player, false);

            if (!queue.contains(qp)) {
                deluxeQueues.getProxyServer().getEventManager().fire(new PlayerQueueEvent(player, server))
                        .thenAcceptAsync(result -> {
                            //Don't add to queue if event cancelled, show player the reason
                            if (result.isCancelled()) {
                                deluxeQueues.getCommandManager().sendMessage(player, MessageType.ERROR,
                                                                             Messages.QUEUES__CANNOT_JOIN);
                                player.sendMessage(TextComponent.of(result.getReason()).color(TextColor.RED));
                                return;
                            }

                            if (player.hasPermission(settingsManager.getProperty(ConfigOptions.DONATOR_PERMISSION))) {
                                queue.addFirst(qp);
                            } else {
                                queue.add(qp);
                            }

                            notifyPlayer(qp);
                        });
            }
        }
    }

    public void removePlayer(QueuePlayer player) {
        queue.remove(player);

        BossBar bossBar = bossBars.get(player.getPlayer().getUniqueId());

        if(bossBar != null) {
            bossBar.removeAllPlayers();
            bossBars.remove(player.getPlayer().getUniqueId());
        }
    }

    public QueuePlayer getFromProxy(Player player) {
        return queue.stream().filter(q -> q.getPlayer() == player).findFirst().orElse(null);
    }

    /**
     * Add in a check to make sure the player can be added to the queue
     * @return added or not
     */
    public boolean canAddPlayer() {
        return server.getPlayersConnected().size() >= playersRequired;
    }

    /**
     * Get the position of a player in a queue
     * @param player the player to check
     * @return their position
     */
    public int getQueuePos(QueuePlayer player) {
        return queue.indexOf(player);
    }

    /**
     * Notify the player that they are in the queue
     * @param player the player to check
     */
    public void notifyPlayer(QueuePlayer player) {
        String actionbar = settingsManager.getProperty(ConfigOptions.ACTIONBAR_DESIGN);
        String message = settingsManager.getProperty(ConfigOptions.TEXT_DESIGN);
        String title_top = settingsManager.getProperty(ConfigOptions.TITLE_HEADER);
        String title_bottom = settingsManager.getProperty(ConfigOptions.TITLE_FOOTER);

        switch (notifyMethod.toLowerCase()) {
            case "bossbar":
                updateBossBar(player);
                break;
            case "actionbar":
                actionbar = actionbar.replace("{server}", server.getServerInfo().getName());
                actionbar = actionbar.replace("{pos}", String.valueOf(getQueuePos(player) + 1));
                actionbar = actionbar.replace("{total}", String.valueOf(queue.size()));
                player.getPlayer().sendMessage(ACFVelocityUtil.color(actionbar), MessagePosition.ACTION_BAR);
                break;
            case "text":
                message = message.replace("{server}", server.getServerInfo().getName());
                message = message.replace("{pos}", String.valueOf(getQueuePos(player) + 1));
                message = message.replace("{total}", String.valueOf(queue.size()));
                player.getPlayer().sendMessage(ACFVelocityUtil.color(message), MessagePosition.SYSTEM);
                break;
            case "title":
                TextTitle.Builder title = TextTitle.builder();
                title.title(ACFVelocityUtil.color(title_top));
                title_bottom = title_bottom.replace("{server}", server.getServerInfo().getName());
                title_bottom = title_bottom.replace("{pos}", String.valueOf(getQueuePos(player) + 1));
                title_bottom = title_bottom.replace("{total}", String.valueOf(queue.size()));
                title.subtitle(ACFVelocityUtil.color(title_bottom));
                player.getPlayer().sendTitle(title.build());
                break;
        }
    }

    public DeluxeQueues getDeluxeQueues() {
        return this.deluxeQueues;
    }

    public LinkedList<QueuePlayer> getQueue() {
        return this.queue;
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

    public int getMaxSlots() {
        return this.maxSlots;
    }

    public SettingsManager getSettingsManager() {
        return this.settingsManager;
    }

    public String getNotifyMethod() {
        return this.notifyMethod;
    }

    public String toString() {
        return "DeluxeQueue(deluxeQueues=" + this.getDeluxeQueues() + ", queue=" + this.getQueue() + ", server=" + this.getServer() + ", delayLength=" + this.getDelayLength() + ", playersRequired=" + this.getPlayersRequired() + ", maxSlots=" + this.getMaxSlots() + ", settingsManager=" + this.getSettingsManager() + ", notifyMethod=" + this.getNotifyMethod() + ")";
    }

    private void updateBossBar(QueuePlayer player) {
        int position = getQueuePos(player) + 1;
        int total = queue.size();

        String message = settingsManager.getProperty(ConfigOptions.BOSSBAR_DESIGN);
        message = message.replace("{server}", server.getServerInfo().getName());
        message = message.replace("{pos}", String.valueOf(position));
        message = message.replace("{total}", String.valueOf(total));

        float progress = (position - 1) /  (float)total;

        BossBar bossBar = bossBars.get(player.getPlayer().getUniqueId());

        if(bossBar != null) {
            bossBar.setTitle(TextComponent.of(message));
            bossBar.setPercent(progress);
        } else {
            bossBar = deluxeQueues.getProxyServer().createBossBar(TextComponent.of(message), BossBarColor.PURPLE, BossBarOverlay.PROGRESS, progress);
            bossBar.addPlayer(player.getPlayer());
            bossBars.put(player.getPlayer().getUniqueId(), bossBar);
        }
    }
}
