package uk.co.notnull.proxyqueues.tasks;

import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import uk.co.notnull.proxyqueues.MessageType;
import uk.co.notnull.proxyqueues.ProxyQueues;
import uk.co.notnull.proxyqueues.configuration.sections.ConfigOptions;
import uk.co.notnull.proxyqueues.queues.ProxyQueue;
import uk.co.notnull.proxyqueues.queues.QueuePlayer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainComponentSerializer;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Created by Glare
 * Date: 7/13/2019
 * Time: 10:47 PM
 */
public class QueueMoveTask implements Runnable {
    private final ProxyQueue queue;
    private final RegisteredServer server;
    private final ProxyQueues proxyQueues;
    private final List<String> fatalErrors;
    private final RegisteredServer waitingServer;

    private QueuePlayer targetPlayer = null;

    public QueueMoveTask(ProxyQueue queue, RegisteredServer server, ProxyQueues proxyQueues) {
        this.queue = queue;
        this.server = server;
        this.proxyQueues = proxyQueues;
        this.fatalErrors = proxyQueues.getSettingsHandler().getSettingsManager().getProperty(
                            ConfigOptions.FATAL_ERRORS);

        waitingServer = proxyQueues.getWaitingServer().orElse(null);
    }

    @Override
    public void run() {
        ConcurrentLinkedQueue<QueuePlayer> normalQueue = queue.getQueue();
        ConcurrentLinkedQueue<QueuePlayer> priorityQueue = queue.getPriorityQueue();
        ConcurrentLinkedQueue<QueuePlayer> staffQueue = queue.getStaffQueue();

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

        // Nothing to do if no player to queue, or connection attempt already underway
        if(targetPlayer == null || targetPlayer.isConnecting()) {
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
                                  + PlainComponentSerializer.plain()
                            .serialize(result.getReasonComponent().orElse(Component.text("(None)"))) + "\"");

            Component reason = result.getReasonComponent().orElse(Component.empty());
            String reasonPlain = PlainComponentSerializer.plain().serialize(reason);
            ServerConnection currentServer = targetPlayer.getPlayer().getCurrentServer().orElse(null);

            boolean fatal = fatalErrors.stream().anyMatch(reasonPlain::contains);

		    if(fatal) {
                if(currentServer == null || currentServer.getServer().equals(waitingServer)) {
                    targetPlayer.getPlayer().disconnect(result.getReasonComponent().orElse(Component.empty()));
                } else {
                    queue.removePlayer(targetPlayer, false);

                    proxyQueues.sendMessage(targetPlayer.getPlayer(), MessageType.ERROR,
                                            "errors.queue-cannot-join", Map.of("{reason}", reasonPlain));
                }
            }
        });

        targetPlayer.setConnecting(true);
    }

    private void handleQueue(ConcurrentLinkedQueue<QueuePlayer> q) {
        int position = 1;
        int disconnectTimeout = proxyQueues.getSettingsHandler()
                .getSettingsManager().getProperty(ConfigOptions.DISCONNECT_TIMEOUT);

        for (QueuePlayer player : q) {
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
