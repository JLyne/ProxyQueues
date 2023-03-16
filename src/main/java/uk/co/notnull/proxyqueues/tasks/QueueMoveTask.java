/*
 * ProxyQueues, a Velocity queueing solution
 * Copyright (c) 2021 James Lyne
 *
 * Some portions of this file were taken from https://github.com/darbyjack/DeluxeQueues
 * These portions are Copyright (c) 2019 Glare
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

package uk.co.notnull.proxyqueues.tasks;

import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import uk.co.notnull.proxyqueues.api.MessageType;
import uk.co.notnull.proxyqueues.ProxyQueuesImpl;
import uk.co.notnull.proxyqueues.configuration.sections.ConfigOptions;
import uk.co.notnull.proxyqueues.queues.ProxyQueueImpl;
import uk.co.notnull.proxyqueues.queues.QueuePlayerImpl;
import net.kyori.adventure.text.Component;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;


public class QueueMoveTask implements Runnable {
    private final ProxyQueueImpl queue;
    private final RegisteredServer server;
    private final ProxyQueuesImpl proxyQueues;
    private final List<String> fatalErrors;
    private final RegisteredServer waitingServer;

    private QueuePlayerImpl targetPlayer = null;

    public QueueMoveTask(ProxyQueueImpl queue, RegisteredServer server, ProxyQueuesImpl proxyQueues) {
        this.queue = queue;
        this.server = server;
        this.proxyQueues = proxyQueues;
        this.fatalErrors = proxyQueues.getSettingsHandler().getSettingsManager().getProperty(
                            ConfigOptions.FATAL_ERRORS);

        waitingServer = proxyQueues.getWaitingServer().orElse(null);
    }

    @Override
    public void run() {
        ConcurrentLinkedQueue<QueuePlayerImpl> normalQueue = queue.getQueue();
        ConcurrentLinkedQueue<QueuePlayerImpl> priorityQueue = queue.getPriorityQueue();
        ConcurrentLinkedQueue<QueuePlayerImpl> staffQueue = queue.getStaffQueue();

        if(targetPlayer != null && (!targetPlayer.isConnecting() || !targetPlayer.getPlayer().isActive())) {
            targetPlayer.setConnecting(false);
            targetPlayer = null;
        }

        if(targetPlayer != null && targetPlayer.getLastConnectionAttempt().isBefore(Instant.now().minusSeconds(10))) {
            proxyQueues.getLogger().debug("Target player timed out");
            targetPlayer.setConnecting(false);
            targetPlayer = null;
        }

        handleQueue(staffQueue);
        handleQueue(priorityQueue);
        handleQueue(normalQueue);

        // Nothing to do if no player to queue, connection attempt already underway, or queue is paused
        if(targetPlayer == null || targetPlayer.isConnecting() || queue.isPaused()) {
            return;
        }

        // Check if the max amount of players on the server are the max slots
        if (queue.isServerFull(targetPlayer.getQueueType())) {
            return;
        }

        connectPlayer();
    }

    private void connectPlayer() {
        proxyQueues.getLogger().info("Attempting to connect player " + targetPlayer.toString());

        targetPlayer.getPlayer().createConnectionRequest(server).connect().thenAcceptAsync(result -> {
            targetPlayer.setConnecting(false);

            if(result.isSuccessful()) {
                return;
            }

            proxyQueues.getLogger()
                    .info("Player " +
                                  targetPlayer.getPlayer().getUsername() + " failed to join "
                                  + queue.getServer().getServerInfo().getName()
                                  + ". Reason: \""
                                  + PlainTextComponentSerializer.plainText()
                            .serialize(result.getReasonComponent().orElse(Component.text("(None)"))) + "\"");

            Component reason = result.getReasonComponent().orElse(Component.empty());
            String reasonPlain = PlainTextComponentSerializer.plainText().serialize(reason);
            ServerConnection currentServer = targetPlayer.getPlayer().getCurrentServer().orElse(null);

            boolean fatal = fatalErrors.stream().anyMatch(reasonPlain::contains);

		    if(fatal) {
                queue.removePlayer(targetPlayer, false);

                if(currentServer == null || currentServer.getServer().equals(waitingServer)) {
                    targetPlayer.getPlayer().disconnect(result.getReasonComponent().orElse(Component.empty()));
                } else {
                    proxyQueues.sendMessage(targetPlayer.getPlayer(), MessageType.ERROR,
                                            "errors.queue-cannot-join",
                                            Collections.singletonMap("reason", reasonPlain));
                }
            }
        });

        targetPlayer.setConnecting(true);
    }

    private void handleQueue(ConcurrentLinkedQueue<QueuePlayerImpl> q) {
        int position = 1;
        int disconnectTimeout = proxyQueues.getSettingsHandler()
                .getSettingsManager().getProperty(ConfigOptions.DISCONNECT_TIMEOUT);

        for (QueuePlayerImpl player : q) {
            boolean online = player.getPlayer().isActive();

            if (online || player.getLastSeen() == null) {
                player.setLastSeen(Instant.now());
            }

            if (!online && player.getLastSeen().isBefore(Instant.now().minusSeconds(disconnectTimeout))) {
                proxyQueues.getLogger()
                        .info("Removing timed out player " + player.getPlayer().getUsername()
                                      + " from " + player.getQueueType() + "/" + position);

                queue.removePlayer(player, false);
                continue;
            }

            if (targetPlayer == null && online) {
                targetPlayer = player;
            }

            player.setPosition(position);
            queue.getNotifier().notifyPlayer(player);
            position++;
        }
    }
}
