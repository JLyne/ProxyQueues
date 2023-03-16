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
import net.kyori.adventure.audience.MessageType;
import net.kyori.adventure.identity.Identity;
import net.kyori.adventure.title.Title;
import uk.co.notnull.platformdetection.PlatformDetectionVelocity;
import uk.co.notnull.proxyqueues.Messages;
import uk.co.notnull.proxyqueues.ProxyQueuesImpl;
import uk.co.notnull.proxyqueues.api.queues.QueuePlayer;
import uk.co.notnull.proxyqueues.configuration.sections.ConfigOptions;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;

public class ProxyQueueNotifier {

    private final ProxyQueueImpl queue;
	private final String notifyMethod;
    private final boolean platformDetectionEnabled;
    private PlatformDetectionVelocity platformDetection;

    public ProxyQueueNotifier(ProxyQueuesImpl proxyQueues, ProxyQueueImpl queue) {
		this.queue = queue;

		notifyMethod = proxyQueues.getSettingsHandler().getSettingsManager().getProperty(ConfigOptions.INFORM_METHOD);

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
        String key;

        switch (player.getQueueType()) {
            case STAFF:
                key = "notify.staff";
                break;

            case PRIORITY:
                key = "notify.priority";
                break;

            case NORMAL:
            default:
                key = "notify.normal";
                break;
        }

        switch (notifyMethod.toLowerCase()) {
            case "bossbar":
                updateBossBar(player);
                break;
            case "actionbar":
                player.getPlayer().sendActionBar(Messages.getComponent(key + ".actionbar", Map.of(
                        "server", queue.getServer().getServerInfo().getName(),
                        "pos", String.valueOf(player.getPosition()),
                        "size", String.valueOf(queue.getQueueSize(player.getQueueType()))
                ), Collections.emptyMap()));
                break;
            case "text":
                player.getPlayer().sendMessage(Messages.getComponent(key + ".chat", Map.of(
                        "server", queue.getServer().getServerInfo().getName(),
                        "pos", String.valueOf(player.getPosition()),
                        "size", String.valueOf(queue.getQueueSize(player.getQueueType()))
                ), Collections.emptyMap()));
                break;
            case "title":
                player.getPlayer().showTitle(
                        Title.title(Messages.getComponent(key + ".title.title"),
                                    Messages.getComponent(key + ".title.subtitle", Map.of(
                                            "server", queue.getServer().getServerInfo().getName(),
                                            "pos", String.valueOf(player.getPosition()),
                                            "size", String.valueOf(queue.getQueueSize(player.getQueueType()))
                                    ), Collections.emptyMap())));
                break;
        }
    }

    private void updateBossBar(QueuePlayer player) {
        int position = player.getPosition();
        String key;

        switch (player.getQueueType()) {
            case STAFF:
                key = "notify.staff.bossbar";
                break;
            case PRIORITY:
                key = "notify.priority.bossbar";
                break;
            case NORMAL:
            default:
                key = "notify.normal.bossbar";
                break;
        }

        if(platformDetectionEnabled && platformDetection.getPlatform(player.getPlayer()).isBedrock()) {
            player.hideBossBar();
        }

        player.showBossBar();
        player.getBossBar().name(
                Messages.getComponent(key, Map.of(
                                              "server", queue.getServer().getServerInfo().getName(),
                                              "pos", String.valueOf(position),
                                              "size", String.valueOf(queue.getQueueSize(player.getQueueType()))),
                                      Collections.emptyMap()));
    }

	public String getNotifyMethod() {
    	return notifyMethod;
	}
}
