package uk.co.notnull.proxyqueues.queues;

import ch.jalu.configme.SettingsManager;
import com.velocitypowered.api.event.PostOrder;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.player.KickedFromServerEvent;
import com.velocitypowered.api.event.player.ServerConnectedEvent;
import com.velocitypowered.api.event.player.ServerPreConnectEvent;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import uk.co.notnull.proxyqueues.ProxyQueues;
import uk.co.notnull.proxyqueues.QueueType;
import uk.co.notnull.proxyqueues.configuration.sections.ConfigOptions;
import net.kyori.text.Component;
import net.kyori.text.TextComponent;
import net.kyori.text.format.TextColor;
import net.kyori.text.serializer.plain.PlainComponentSerializer;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ProxyQueueEventHandler {
	private final ProxyQueues proxyQueues;
	private final SettingsManager settingsManager;
	private final ProxyQueue queue;

	private final Set<UUID> fatalKicks = ConcurrentHashMap.newKeySet();

	public ProxyQueueEventHandler(ProxyQueues proxyQueues, ProxyQueue queue) {
		this.settingsManager = proxyQueues.getSettingsHandler().getSettingsManager();
		this.proxyQueues = proxyQueues;
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
				Optional<RegisteredServer> waitingServer = proxyQueues.getWaitingServer();

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

        proxyQueues.getLogger().info("Handling proxy disconnect for " + player.getUsername());

        if (queuePlayer.isPresent()) {
            proxyQueues.getLogger().info("Player in queue, setting last seen");
            queuePlayer.get().setLastSeen(Instant.now());
            return;
        }

        player.getCurrentServer().ifPresent(server -> {
			if(server.getServer().equals(queue.getServer())) {
				if(fatalKicks.contains(player.getUniqueId())) {
					proxyQueues.getLogger().info(
						"Player disconnecting due to fatal kick. Not re-queueing.");

					fatalKicks.remove(player.getUniqueId());

					return;
				}

				boolean staff = player.hasPermission(settingsManager.getProperty(ConfigOptions.STAFF_PERMISSION));
				proxyQueues.getLogger().info(
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

        proxyQueues.getLogger()
                .info("Player " + event.getPlayer().getUsername() + " kicked from " +
                              event.getServer().getServerInfo().getName() + ". Reason: " + event.getOriginalReason());

		Component reason = event.getOriginalReason().orElse(TextComponent.empty());
		String reasonPlain = PlainComponentSerializer.INSTANCE.serialize(reason);
		List<String> fatalErrors = proxyQueues.getSettingsHandler().getSettingsManager().getProperty(
				ConfigOptions.FATAL_ERRORS);

		boolean fatal = fatalErrors.stream().anyMatch(reasonPlain::contains);

		if(!fatal) {
			proxyQueues.getLogger()
			.info("Reason not fatal, re-queueing " + event.getPlayer().getUsername());

			boolean staff = event.getPlayer()
					.hasPermission(settingsManager.getProperty(ConfigOptions.STAFF_PERMISSION));

			queue.addPlayer(event.getPlayer(), staff ? QueueType.STAFF : QueueType.PRIORITY);
		} else {
			proxyQueues.getLogger().info("Reason fatal");
			fatalKicks.add(event.getPlayer().getUniqueId());
		}
    }
}
