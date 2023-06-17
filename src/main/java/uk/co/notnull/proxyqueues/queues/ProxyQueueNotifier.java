/*
 * ProxyQueues, a Velocity queueing solution
 *
 * Copyright (c) 2022 James Lyne
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

import com.velocitypowered.api.plugin.PluginContainer;
import com.velocitypowered.api.proxy.InboundConnection;
import net.kyori.adventure.title.Title;
import org.jetbrains.annotations.Nullable;
import uk.co.notnull.platformdetection.PlatformDetectionVelocity;
import uk.co.notnull.proxyqueues.Messages;
import uk.co.notnull.proxyqueues.ProxyQueuesImpl;
import uk.co.notnull.proxyqueues.api.MessageType;
import uk.co.notnull.proxyqueues.api.queues.QueuePlayer;
import uk.co.notnull.proxyqueues.configuration.sections.ConfigOptions;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class ProxyQueueNotifier {

    private final ProxyQueuesImpl plugin;
    private final ProxyQueueImpl queue;
	private final String notifyMethod;
    private final boolean platformDetectionEnabled;
    private PlatformDetectionVelocity platformDetection;

    public ProxyQueueNotifier(ProxyQueuesImpl plugin, ProxyQueueImpl queue) {
		this.plugin = plugin;
		this.queue = queue;

		notifyMethod = plugin.getSettingsHandler().getSettingsManager().getProperty(ConfigOptions.INFORM_METHOD);

		Optional<PluginContainer> platformDetection = plugin.getProxyServer().getPluginManager()
                .getPlugin("platform-detection");
        platformDetectionEnabled = platformDetection.isPresent();

        if(platformDetectionEnabled) {
            this.platformDetection = (PlatformDetectionVelocity) platformDetection.orElseThrow().getInstance()
                    .orElseThrow();
        }
	}

	/**
     * Notify the player that they are in the queue
     * @param player the player to check
     */
    public void notifyPlayer(QueuePlayer player) {
        StringBuilder key = switch (player.getQueueType()) {
            case STAFF -> new StringBuilder("notify.staff");
            case PRIORITY -> new StringBuilder("notify.priority");
            default -> new StringBuilder("notify.normal");
        };

        switch (notifyMethod.toLowerCase()) {
            case "bossbar" -> updateBossBar(player);
            case "actionbar" -> {
                key.append(".actionbar").append(queue.isPaused() ? ".paused" : ".active");
                player.getPlayer().sendActionBar(Messages.getComponent(key.toString(), Map.of(
                        "server", queue.getServer().getServerInfo().getName(),
                        "pos", String.valueOf(player.getPosition()),
                        "size", String.valueOf(queue.getQueueSize(player.getQueueType()))
                ), Collections.emptyMap()));
            }
            case "text" -> {
                key.append(".chat").append(queue.isPaused() ? ".paused" : ".active");
                player.getPlayer().sendMessage(Messages.getComponent(key.toString(), Map.of(
                        "server", queue.getServer().getServerInfo().getName(),
                        "pos", String.valueOf(player.getPosition()),
                        "size", String.valueOf(queue.getQueueSize(player.getQueueType()))
                ), Collections.emptyMap()));
            }
            case "title" -> {
                key.append(".title").append(queue.isPaused() ? ".paused" : ".active");
                player.getPlayer().showTitle(
                        Title.title(Messages.getComponent(key + ".title"),
                                    Messages.getComponent(key + ".subtitle", Map.of(
                                            "server", queue.getServer().getServerInfo().getName(),
                                            "pos", String.valueOf(player.getPosition()),
                                            "size", String.valueOf(queue.getQueueSize(player.getQueueType()))
                                    ), Collections.emptyMap())));
            }
        }
    }

    private void updateBossBar(QueuePlayer player) {
        int position = player.getPosition();
        String key = switch (player.getQueueType()) {
            case STAFF -> queue.isPaused() ? "notify.staff.bossbar.paused" : "notify.staff.bossbar.active";
            case PRIORITY -> queue.isPaused() ? "notify.priority.bossbar.paused" : "notify.priority.bossbar.active";
            default -> queue.isPaused() ? "notify.normal.bossbar.paused" : "notify.normal.bossbar.active";
        };

        if(platformDetectionEnabled && platformDetection.getPlatform(player.getPlayer()).isBedrock()) {
            player.hideBossBar();
        }

        player.showBossBar();
        player.getBossBar().name(
                Messages.getComponent(key, Map.of(
                                              "server", queue.getServer().getServerInfo().getName(),
                                              "pos", String.valueOf(position),
                                              "size", String.valueOf(queue.getQueueSize(player.getQueueType()))),
                                      Collections.emptyMap())).color(player.getBossBarColor());
    }

	public String getNotifyMethod() {
    	return notifyMethod;
	}

    public void notifyPause() {
        notifyPause(null);
    }

    public void notifyPause(@Nullable QueuePlayer player) {
        Map<PluginContainer, String> pauses = queue.getPauses();
        String reasons = pauses.values().stream()
                .map(r -> Messages.get("notify.pause-reason", Collections.singletonMap("reason", r)))
                .collect(Collectors.joining("\n"));

        String message = Messages.getPrefixed("notify.paused", MessageType.WARNING,
                                                          Map.of(
                                                          "server", queue.getServer().getServerInfo().getName(),
                                                          "reasons", reasons));

        if(player != null) {
            plugin.sendMessage(player.getPlayer(), message);
        } else {
            notifyAll(message);
        }
    }

    public void notifyResume() {
        notifyResume(null);
    }

    public void notifyResume(@Nullable QueuePlayer player) {
        String message = Messages.getPrefixed(
                "notify.unpaused", MessageType.INFO,
                Collections.singletonMap("server", queue.getServer().getServerInfo().getName()));

        if(player != null) {
            plugin.sendMessage(player.getPlayer(), message);
        } else {
            notifyAll(message);
        }
    }

    private void notifyAll(String message) {
        queue.getQueue().stream().map(QueuePlayerImpl::getPlayer).filter(InboundConnection::isActive)
                .forEach(p -> plugin.sendMessage(p, message));
        queue.getPriorityQueue().stream().map(QueuePlayerImpl::getPlayer).filter(InboundConnection::isActive)
                .forEach(p -> plugin.sendMessage(p, message));
        queue.getStaffQueue().stream().map(QueuePlayerImpl::getPlayer).filter(InboundConnection::isActive)
                .forEach(p -> plugin.sendMessage(p, message));
    }
}
