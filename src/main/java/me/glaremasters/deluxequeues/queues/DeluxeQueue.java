package me.glaremasters.deluxequeues.queues;

import ch.jalu.configme.SettingsManager;
import co.aikar.commands.ACFBungeeUtil;
import co.aikar.commands.BungeeCommandIssuer;
import co.aikar.commands.CommandIssuer;
import co.aikar.commands.MessageType;
import me.glaremasters.deluxequeues.DeluxeQueues;
import me.glaremasters.deluxequeues.configuration.sections.ConfigOptions;
import me.glaremasters.deluxequeues.events.PlayerQueueEvent;
import me.glaremasters.deluxequeues.messages.Messages;
import me.glaremasters.deluxequeues.tasks.QueueMoveTask;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.Title;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;

import java.util.LinkedList;
import java.util.concurrent.TimeUnit;

/**
 * Created by Glare
 * Date: 7/13/2019
 * Time: 10:30 PM
 */
public class DeluxeQueue {

    private DeluxeQueues deluxeQueues;
    private LinkedList<QueuePlayer> queue = new LinkedList<>();
    private ServerInfo server;
    private int delayLength;
    private int playersRequired;
    private int maxSlots;
    private SettingsManager settingsManager;
    private String notifyMethod;

    public DeluxeQueue(DeluxeQueues deluxeQueues, ServerInfo server, int playersRequired, int maxSlots) {
        this.deluxeQueues = deluxeQueues;
        this.server = server;
        this.settingsManager = deluxeQueues.getSettingsHandler().getSettingsManager();
        this.delayLength = settingsManager.getProperty(ConfigOptions.DELAY_LENGTH);
        this.playersRequired = playersRequired;
        this.maxSlots = maxSlots;
        this.notifyMethod = settingsManager.getProperty(ConfigOptions.INFORM_METHOD);
        deluxeQueues.getProxy().getScheduler().schedule(deluxeQueues, new QueueMoveTask(this, server), 0, delayLength, TimeUnit.SECONDS);
    }

    /**
     * Add a player to a queue
     * @param player the player to add
     */
    public void addPlayer(ProxiedPlayer player) {
        if (getFromProxy(player) == null) {
            QueuePlayer qp = new QueuePlayer(player, false);

            if (queue.contains(qp)) {
                return;
            }

            PlayerQueueEvent queueEvent = deluxeQueues.getProxy().getPluginManager().callEvent(new PlayerQueueEvent(player, server));

            //Don't add to queue if event cancelled, show player the reason
            if(queueEvent.isCancelled()) {
                deluxeQueues.getCommandManager().sendMessage(player, MessageType.ERROR, Messages.QUEUES__CANNOT_JOIN);
                player.sendMessage(new ComponentBuilder(queueEvent.getReason()).color(ChatColor.RED).create());
                return;
            }

            if (player.hasPermission(settingsManager.getProperty(ConfigOptions.DONATOR_PERMISSION))) {
                queue.addFirst(qp);
            }
            else {
                queue.add(qp);
            }
        }
    }

    public void removePlayer(QueuePlayer player) {
        queue.remove(player);
    }

    public QueuePlayer getFromProxy(ProxiedPlayer player) {
        return queue.stream().filter(q -> q.getPlayer() == player).findFirst().orElse(null);
    }

    /**
     * Add in a check to make sure the player can be added to the queue
     * @return added or not
     */
    public boolean canAddPlayer() {
        return server.getPlayers().size() >= playersRequired;
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
            case "actionbar":
                actionbar = actionbar.replace("{server}", server.getName());
                actionbar = actionbar.replace("{pos}", String.valueOf(getQueuePos(player) + 1));
                actionbar = actionbar.replace("{total}", String.valueOf(queue.size()));
                player.getPlayer().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ACFBungeeUtil.color(actionbar)));
                break;
            case "text":
                message = message.replace("{server}", server.getName());
                message = message.replace("{pos}", String.valueOf(getQueuePos(player) + 1));
                message = message.replace("{total}", String.valueOf(queue.size()));
                player.getPlayer().sendMessage(new TextComponent(ACFBungeeUtil.color(message)));
                break;
            case "title":
                Title title = deluxeQueues.getProxy().createTitle();
                title.title(new TextComponent(ACFBungeeUtil.color(title_top)));
                title_bottom = title_bottom.replace("{server}", server.getName());
                title_bottom = title_bottom.replace("{pos}", String.valueOf(getQueuePos(player) + 1));
                title_bottom = title_bottom.replace("{total}", String.valueOf(queue.size()));
                title.subTitle(new TextComponent(ACFBungeeUtil.color(title_bottom)));
                player.getPlayer().sendTitle(title);
                break;
        }
    }

    /**
     * Check if the queue is holding a player
     * @param player the player to check for
     * @return in the queue or not
     */
    public int checkForPlayer(ProxiedPlayer player) {
        return queue.indexOf(player);
    }

    public DeluxeQueues getDeluxeQueues() {
        return this.deluxeQueues;
    }

    public LinkedList<QueuePlayer> getQueue() {
        return this.queue;
    }

    public ServerInfo getServer() {
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
}
