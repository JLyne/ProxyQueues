/*
 * ProxyQueues, a Velocity queueing solution
 *
 * Copyright (c) 2021 James Lyne
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package uk.co.notnull.proxyqueues.queues;

import ch.jalu.configme.SettingsManager;
import com.velocitypowered.api.event.PostOrder;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.player.KickedFromServerEvent;
import com.velocitypowered.api.event.player.ServerPostConnectEvent;
import com.velocitypowered.api.event.player.ServerPreConnectEvent;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import uk.co.notnull.proxyqueues.Messages;
import uk.co.notnull.proxyqueues.ProxyQueuesImpl;
import uk.co.notnull.proxyqueues.api.QueueType;
import uk.co.notnull.proxyqueues.api.queues.QueuePlayer;
import uk.co.notnull.proxyqueues.configuration.sections.ConfigOptions;

import java.time.Instant;
import java.util.*;

public class ProxyQueueEventHandler {
	private final ProxyQueuesImpl proxyQueues;
	private final SettingsManager settingsManager;
	private final ProxyQueueImpl queue;

	public ProxyQueueEventHandler(ProxyQueuesImpl proxyQueues, ProxyQueueImpl queue) {
		this.settingsManager = proxyQueues.getSettingsHandler().getSettingsManager();
		this.proxyQueues = proxyQueues;
		this.queue = queue;
	}

	@Subscribe(priority = Short.MIN_VALUE / 2)
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

        if(queuePlayer.isEmpty()) {
            if(event.getPlayer().getCurrentServer().isEmpty()) {
				Optional<RegisteredServer> waitingServer = proxyQueues.getWaitingServer();

                if(waitingServer.isPresent()) {
                    event.setResult(ServerPreConnectEvent.ServerResult.allowed(waitingServer.get()));
                } else {
                    player.disconnect(Messages.getComponent("cannot-connect-directly"));

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

	@Subscribe(priority = Short.MIN_VALUE / 2)
    public void onConnected(ServerPostConnectEvent event) {
		RegisteredServer server = event.getPlayer().getCurrentServer()
				.map(ServerConnection::getServer).orElse(null);

		if(server != null) {
			if(server.equals(queue.getServer())) {
				queue.removePlayer(event.getPlayer(), true);
			} else {
				queue.getQueuePlayer(event.getPlayer(), true).ifPresent(p -> {
					queue.getNotifier().notifyPlayer(p);
				});
			}
		}

        RegisteredServer previousServer = event.getPreviousServer();

        if(previousServer != null && previousServer.equals(this.queue.getServer())) {
			queue.clearConnectedState(event.getPlayer());
        }
    }

    /**
     * Handles a player disconnecting from the proxy
     * If they are queued their lastSeen time will be set
     * Otherwise they will be added to the priority queue with a lastSeen time set
     * @param event - The event
     */
    @Subscribe(priority = Short.MAX_VALUE / 2)
    public void onLeave(DisconnectEvent event) {
        Player player = event.getPlayer();
        Optional<QueuePlayer> queuePlayer = queue.getQueuePlayer(player, false);
        queue.clearConnectedState(player);

        proxyQueues.getLogger().debug("Handling proxy disconnect for " + player.getUsername());

        if (queuePlayer.isPresent()) {
            proxyQueues.getLogger().debug("Player in queue, setting last seen");
			((QueuePlayerImpl) queuePlayer.get()).setLastSeen(Instant.now());
            return;
        }

        player.getCurrentServer().ifPresent(server -> {
			if(server.getServer().equals(queue.getServer())) {
				boolean staff = player.hasPermission(settingsManager.getProperty(ConfigOptions.STAFF_PERMISSION));
				proxyQueues.getLogger().debug(
						"Player not in queue, adding to " + (staff ? "staff" : "priority") + " queue");
				queue.addPlayer(player, staff ? QueueType.STAFF : QueueType.PRIORITY);
			}
        });
    }

    @Subscribe(priority = Short.MIN_VALUE + 1)
    public void onKick(KickedFromServerEvent event) {
		if (!event.getServer().equals(queue.getServer()) || event.kickedDuringServerConnect()) {
			return;
		}

		proxyQueues.getLogger()
				.debug("Player " + event.getPlayer().getUsername() + " kicked from " +
							   event.getServer().getServerInfo().getName() + ". Reason: " + event.getServerKickReason());

		Component reason = event.getServerKickReason().orElse(Component.empty());
		String reasonPlain = PlainTextComponentSerializer.plainText().serialize(reason);
		List<String> fatalErrors = proxyQueues.getSettingsHandler().getSettingsManager().getProperty(
				ConfigOptions.FATAL_ERRORS);
		Optional<RegisteredServer> waitingServer = proxyQueues.getWaitingServer();

		boolean fatal = fatalErrors.stream().anyMatch(reasonPlain::contains);

		if (!fatal) {
			proxyQueues.getLogger()
					.debug("Reason not fatal, re-queueing " + event.getPlayer().getUsername());

			boolean staff = event.getPlayer()
					.hasPermission(settingsManager.getProperty(ConfigOptions.STAFF_PERMISSION));

			queue.addPlayer(event.getPlayer(), staff ? QueueType.STAFF : QueueType.PRIORITY);

			if (event.getResult() instanceof KickedFromServerEvent.DisconnectPlayer && waitingServer.isPresent()) {
				event.setResult(KickedFromServerEvent.RedirectPlayer.create(waitingServer.get()));
			}
		} else {
			proxyQueues.getLogger().debug("Reason fatal");
		}
	}
}
