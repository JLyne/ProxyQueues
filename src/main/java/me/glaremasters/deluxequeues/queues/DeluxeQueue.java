package me.glaremasters.deluxequeues.queues;

import ch.jalu.configme.SettingsManager;
import lombok.Getter;
import me.glaremasters.deluxequeues.DeluxeQueues;
import me.glaremasters.deluxequeues.configuration.sections.ConfigOptions;
import me.glaremasters.deluxequeues.tasks.QueueMoveTask;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;

import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.TimeUnit;

/**
 * Created by Glare
 * Date: 7/13/2019
 * Time: 10:30 PM
 */
@Getter
public class DeluxeQueue {

    private DeluxeQueues deluxeQueues;
    private Queue queue;
    private ServerInfo server;
    private int delayLength;
    private int playersRequired;
    private SettingsManager settingsManager;

    public DeluxeQueue(DeluxeQueues deluxeQueues, ServerInfo server, int delayLength) {
        this.deluxeQueues = deluxeQueues;
        this.queue = new LinkedList();
        this.server = server;
        this.delayLength = delayLength;
        this.settingsManager = deluxeQueues.getSettingsHandler().getSettingsManager();
        this.playersRequired = settingsManager.getProperty(ConfigOptions.PLAYERS_REQUIRED);

        deluxeQueues.getProxy().getScheduler().schedule(deluxeQueues, new QueueMoveTask(deluxeQueues, this, server), 0, delayLength, TimeUnit.SECONDS);
    }

    public void addPlayer(ProxiedPlayer player) {
        if (playersRequired > deluxeQueues.getProxy().getPlayers().size()) {

        }
    }

    public void notifyPlayer() {

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
