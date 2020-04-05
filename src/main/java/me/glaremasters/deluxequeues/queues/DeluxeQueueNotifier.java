package me.glaremasters.deluxequeues.queues;

import ch.jalu.configme.SettingsManager;
import co.aikar.commands.ACFVelocityUtil;
import com.velocitypowered.api.util.MessagePosition;
import com.velocitypowered.api.util.title.TextTitle;
import me.glaremasters.deluxequeues.DeluxeQueues;
import me.glaremasters.deluxequeues.configuration.sections.ConfigOptions;
import net.kyori.text.TextComponent;

public class DeluxeQueueNotifier {

	private final SettingsManager settingsManager;
	private final DeluxeQueue queue;
	private final String notifyMethod;

	public DeluxeQueueNotifier(DeluxeQueues deluxeQueues, DeluxeQueue queue) {
		this.settingsManager = deluxeQueues.getSettingsHandler().getSettingsManager();
		this.queue = queue;

		notifyMethod =settingsManager.getProperty(ConfigOptions.INFORM_METHOD);
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
                actionbar = actionbar.replace("{server}", queue.getServer().getServerInfo().getName());
                actionbar = actionbar.replace("{pos}", String.valueOf(player.getPosition()));
                player.getPlayer().sendMessage(ACFVelocityUtil.color(actionbar), MessagePosition.ACTION_BAR);
                break;
            case "text":
                message = message.replace("{server}", queue.getServer().getServerInfo().getName());
                message = message.replace("{pos}", String.valueOf(player.getPosition()));
                player.getPlayer().sendMessage(ACFVelocityUtil.color(message), MessagePosition.SYSTEM);
                break;
            case "title":
                TextTitle.Builder title = TextTitle.builder();
                title.title(ACFVelocityUtil.color(title_top));
                title_bottom = title_bottom.replace("{server}", queue.getServer().getServerInfo().getName());
                title_bottom = title_bottom.replace("{pos}", String.valueOf(player.getPosition()));
                title.subtitle(ACFVelocityUtil.color(title_bottom));
                player.getPlayer().sendTitle(title.build());
                break;
        }
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

        message = message.replace("{server}", queue.getServer().getServerInfo().getName());
        message = message.replace("{pos}", String.valueOf(position));

        player.getBossBar().setVisible(true);
        player.getBossBar().setTitle(TextComponent.of(message));
    }

	public String getNotifyMethod() {
    	return notifyMethod;
	}
}
