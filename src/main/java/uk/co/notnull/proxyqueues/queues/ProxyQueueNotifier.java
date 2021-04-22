package uk.co.notnull.proxyqueues.queues;

import ch.jalu.configme.SettingsManager;
import com.velocitypowered.api.plugin.PluginContainer;
import net.kyori.adventure.audience.MessageType;
import net.kyori.adventure.identity.Identity;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.title.Title;
import uk.co.notnull.platformdetection.PlatformDetectionVelocity;
import uk.co.notnull.proxyqueues.ProxyQueues;
import uk.co.notnull.proxyqueues.configuration.sections.ConfigOptions;

import java.util.Optional;

public class ProxyQueueNotifier {

	private final SettingsManager settingsManager;
	private final ProxyQueue queue;
	private final String notifyMethod;
    private final boolean platformDetectionEnabled;
    private PlatformDetectionVelocity platformDetection;

    public ProxyQueueNotifier(ProxyQueues proxyQueues, ProxyQueue queue) {
		this.settingsManager = proxyQueues.getSettingsHandler().getSettingsManager();
		this.queue = queue;

		notifyMethod =settingsManager.getProperty(ConfigOptions.INFORM_METHOD);

		Optional<PluginContainer> platformDetection = proxyQueues.getProxyServer().getPluginManager()
                .getPlugin("platform-detection");
        platformDetectionEnabled = platformDetection.isPresent();

        if(platformDetectionEnabled) {
            this.platformDetection = (PlatformDetectionVelocity) platformDetection.get().getInstance().get();
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
        LegacyComponentSerializer serializer = LegacyComponentSerializer.legacyAmpersand();

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
                player.getPlayer().sendActionBar(serializer.deserialize(actionbar));
                break;
            case "text":
                message = message.replace("{server}", queue.getServer().getServerInfo().getName());
                message = message.replace("{pos}", String.valueOf(player.getPosition()));
                player.getPlayer().sendMessage(Identity.nil(), serializer.deserialize(message), MessageType.SYSTEM);
                break;
            case "title":
                title_bottom = title_bottom.replace("{server}", queue.getServer().getServerInfo().getName());
                title_bottom = title_bottom.replace("{pos}", String.valueOf(player.getPosition()));
                Title title = Title.title(serializer.deserialize(title_top), serializer.deserialize(title_bottom));
                player.getPlayer().showTitle(title);
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

        if(platformDetectionEnabled && platformDetection.getPlatform(player.getPlayer()).isBedrock()) {
            player.hideBossBar();
        }

        player.showBossBar();
        player.getBossBar().name(Component.text(message));
    }

	public String getNotifyMethod() {
    	return notifyMethod;
	}
}
