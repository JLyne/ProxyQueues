package me.glaremasters.deluxequeues.queues;

import ch.jalu.configme.SettingsManager;
import com.velocitypowered.api.event.PostOrder;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.player.KickedFromServerEvent;
import com.velocitypowered.api.event.player.ServerConnectedEvent;
import com.velocitypowered.api.event.player.ServerPreConnectEvent;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import me.glaremasters.deluxequeues.DeluxeQueues;
import me.glaremasters.deluxequeues.QueueType;
import me.glaremasters.deluxequeues.configuration.sections.ConfigOptions;
import net.kyori.text.Component;
import net.kyori.text.TextComponent;
import net.kyori.text.format.TextColor;
import net.kyori.text.serializer.plain.PlainComponentSerializer;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class DeluxeQueueEventHandler {
	private final DeluxeQueues deluxeQueues;
	private final SettingsManager settingsManager;
	private final DeluxeQueue queue;

	private final Set<UUID> fatalKicks = ConcurrentHashMap.newKeySet();

	public DeluxeQueueEventHandler(DeluxeQueues deluxeQueues, DeluxeQueue queue) {
		this.settingsManager = deluxeQueues.getSettingsHandler().getSettingsManager();
		this.deluxeQueues = deluxeQueues;
		this.queue = queue;
	}

	@Subscribe(order = PostOrder.LATE)
    public void onPreConnect(ServerPreConnectEvent event) {
        if(!event.getResult().isAllowed()) {
            return;
        }

        RegisteredServer server = event.getOriginalServer();
        Player player = event.getPlayer();
        RegisteredServer redirected = event.getResult().getServer().orElse(null);

        if(redirected != null && !redirected.equals(server)) {
            server = redirected;
        }

        // Check if the server has a queue
        if(!server.equals(queue.getServer()) || !this.queue.isActive()) {
            return;
        }

        // Get the queue
        Optional<QueuePlayer> queuePlayer = queue.getQueuePlayer(player, true);

        if(!queuePlayer.isPresent()) {
            if(!event.getPlayer().getCurrentServer().isPresent()) {
				Optional<RegisteredServer> waitingServer = deluxeQueues.getWaitingServer();

                if(waitingServer.isPresent()) {
                    event.setResult(ServerPreConnectEvent.ServerResult.allowed(waitingServer.get()));
                } else {
                    player.disconnect(TextComponent.of(
                            "This server has queueing enabled and can't be connected to directly. Please connect via minecraft.rtgame.co.uk")
                                              .color(TextColor.RED));

                    return;
                }
            } else {
                // Cancel the event so they don't go right away
                event.setResult(ServerPreConnectEvent.ServerResult.denied());
            }

            // Add the player to the queue
            queue.addPlayer(player);
        } else if(!queuePlayer.get().isConnecting()) {
			event.setResult(ServerPreConnectEvent.ServerResult.denied());
		}
    }

	@Subscribe(order = PostOrder.LATE)
    public void onConnected(ServerConnectedEvent event) {
		fatalKicks.remove(event.getPlayer().getUniqueId());

		if(event.getServer().equals(queue.getServer())) {
			queue.removePlayer(event.getPlayer(), true);
		}

        Optional<RegisteredServer> previousServer = event.getPreviousServer();

        previousServer.ifPresent(server -> {
            if(server.equals(this.queue.getServer())) {
                queue.clearConnectedState(event.getPlayer());
            }
        });
    }

    /**
     * Handles a player disconnecting from the proxy
     * If they are queued their lastSeen time will be set
     * Otherwise they will be added to the priority queue with a lastSeen time set
     * @param event - The event
     */
    @Subscribe(order = PostOrder.EARLY)
    public void onLeave(DisconnectEvent event) {
        Player player = event.getPlayer();
        Optional<QueuePlayer> queuePlayer = queue.getQueuePlayer(player, false);
        queue.clearConnectedState(player);

        deluxeQueues.getLogger().info("Handling proxy disconnect for " + player.getUsername());

        if (queuePlayer.isPresent()) {
            deluxeQueues.getLogger().info("Player in queue, setting last seen");
            queuePlayer.get().setLastSeen(Instant.now());
            return;
        }

        player.getCurrentServer().ifPresent(server -> {
			if(server.getServer().equals(queue.getServer())) {
				if(fatalKicks.contains(player.getUniqueId())) {
					deluxeQueues.getLogger().info(
						"Player disconnecting due to fatal kick. Not re-queueing.");

					fatalKicks.remove(player.getUniqueId());

					return;
				}

				boolean staff = player.hasPermission(settingsManager.getProperty(ConfigOptions.STAFF_PERMISSION));
				deluxeQueues.getLogger().info(
						"Player not in queue, adding to " + (staff ? "staff" : "priority") + " queue");
				queue.addPlayer(player, staff ? QueueType.STAFF : QueueType.PRIORITY);
			}
        });
    }

    @Subscribe(order = PostOrder.LAST)
    public void onKick(KickedFromServerEvent event) {
        if(!event.getServer().equals(queue.getServer()) || event.kickedDuringServerConnect()) {
            return;
        }

        deluxeQueues.getLogger()
                .info("Player " + event.getPlayer().getUsername() + " kicked from " +
                              event.getServer().getServerInfo().getName() + ". Reason: " + event.getOriginalReason());

		Component reason = event.getOriginalReason().orElse(TextComponent.empty());
		String reasonPlain = PlainComponentSerializer.INSTANCE.serialize(reason);
		List<String> fatalErrors = deluxeQueues.getSettingsHandler().getSettingsManager().getProperty(
				ConfigOptions.FATAL_ERRORS);

		boolean fatal = fatalErrors.stream().anyMatch(reasonPlain::contains);

		if(!fatal) {
			deluxeQueues.getLogger()
			.info("Reason not fatal, re-queueing " + event.getPlayer().getUsername());

			boolean staff = event.getPlayer()
					.hasPermission(settingsManager.getProperty(ConfigOptions.STAFF_PERMISSION));

			queue.addPlayer(event.getPlayer(), staff ? QueueType.STAFF : QueueType.PRIORITY);
		} else {
			deluxeQueues.getLogger().info("Reason fatal");
			fatalKicks.add(event.getPlayer().getUniqueId());
		}
    }
}
