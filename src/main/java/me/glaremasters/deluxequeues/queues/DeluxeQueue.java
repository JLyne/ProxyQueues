package me.glaremasters.deluxequeues.queues;

import ch.jalu.configme.SettingsManager;
import co.aikar.commands.ACFBungeeUtil;
import lombok.Getter;
import lombok.ToString;
import me.glaremasters.deluxequeues.DeluxeQueues;
import me.glaremasters.deluxequeues.configuration.sections.ConfigOptions;
import me.glaremasters.deluxequeues.tasks.QueueMoveTask;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.Title;
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
@Getter
@ToString
public class DeluxeQueue {

    private DeluxeQueues deluxeQueues;
    private LinkedList<ProxiedPlayer> queue = new LinkedList<>();
    private ServerInfo server;
    private int delayLength;
    private int playersRequired;
    private int maxSlots;
    private SettingsManager settingsManager;

    public DeluxeQueue(DeluxeQueues deluxeQueues, ServerInfo server, int playersRequired, int maxSlots) {
        this.deluxeQueues = deluxeQueues;
        this.server = server;
        this.settingsManager = deluxeQueues.getSettingsHandler().getSettingsManager();
        this.delayLength = settingsManager.getProperty(ConfigOptions.DELAY_LENGTH);
        this.playersRequired = playersRequired;
        this.maxSlots = maxSlots;

        deluxeQueues.getProxy().getScheduler().schedule(deluxeQueues, new QueueMoveTask(this, server), 0, delayLength, TimeUnit.SECONDS);
    }

    /**
     * Add a player to a queue
     * @param player the player to add
     */
    public void addPlayer(ProxiedPlayer player) {
            if (!queue.contains(player)) {
                queue.add(player);
                notifyPlayer(player);
        }
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
    public int getQueuePos(ProxiedPlayer player) {
        return queue.indexOf(player);
    }

    /**
     * Notify the player that they are in the queue
     * @param player the player to check
     */
    public void notifyPlayer(ProxiedPlayer player) {
        String method = settingsManager.getProperty(ConfigOptions.INFORM_METHOD);
        String actionbar = settingsManager.getProperty(ConfigOptions.ACTIONBAR_DESIGN);
        String message = settingsManager.getProperty(ConfigOptions.TEXT_DESIGN);
        String title_top = settingsManager.getProperty(ConfigOptions.TITLE_HEADER);
        String title_bottom = settingsManager.getProperty(ConfigOptions.TITLE_FOOTER);
        switch (method.toLowerCase()) {
            case "actionbar":
                actionbar = actionbar.replace("{pos}", String.valueOf(getQueuePos(player) + 1));
                actionbar = actionbar.replace("{total}", String.valueOf(queue.size()));
                player.sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ACFBungeeUtil.color(actionbar)));
                break;
            case "text":
                message = message.replace("{pos}", String.valueOf(getQueuePos(player) + 1));
                message = message.replace("{total}", String.valueOf(queue.size()));
                player.sendMessage(new TextComponent(ACFBungeeUtil.color(message)));
                break;
            case "title":
                Title title = deluxeQueues.getProxy().createTitle();
                title.title(new TextComponent(ACFBungeeUtil.color(title_top)));
                title_bottom = title_bottom.replace("{pos}", String.valueOf(getQueuePos(player) + 1));
                title_bottom = title_bottom.replace("{total}", String.valueOf(queue.size()));
                title.subTitle(new TextComponent(ACFBungeeUtil.color(title_bottom)));
                player.sendTitle(title);
                break;
        }
    }

    /**
     * Check if the queue is holding a player
     * @param player the player to check for
     * @return in the queue or not
     */
    public boolean checkForPlayer(ProxiedPlayer player) {
        return queue.contains(player);
    }

}
