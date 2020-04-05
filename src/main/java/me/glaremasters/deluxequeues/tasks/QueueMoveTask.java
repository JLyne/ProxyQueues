package me.glaremasters.deluxequeues.tasks;

import co.aikar.commands.MessageType;
import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import me.glaremasters.deluxequeues.DeluxeQueues;
import me.glaremasters.deluxequeues.configuration.sections.ConfigOptions;
import me.glaremasters.deluxequeues.messages.Messages;
import me.glaremasters.deluxequeues.queues.DeluxeQueue;
import me.glaremasters.deluxequeues.queues.QueuePlayer;
import net.kyori.text.Component;
import net.kyori.text.TextComponent;
import net.kyori.text.serializer.plain.PlainComponentSerializer;

import java.time.Instant;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Created by Glare
 * Date: 7/13/2019
 * Time: 10:47 PM
 */
public class QueueMoveTask implements Runnable {
    private final DeluxeQueue queue;
    private final RegisteredServer server;
    private final DeluxeQueues deluxeQueues;
    private final List<String> fatalErrors;
    private final RegisteredServer waitingServer;

    private QueuePlayer targetPlayer = null;

    public QueueMoveTask(DeluxeQueue queue, RegisteredServer server, DeluxeQueues deluxeQueues) {
        this.queue = queue;
        this.server = server;
        this.deluxeQueues = deluxeQueues;
        this.fatalErrors = deluxeQueues.getSettingsHandler().getSettingsManager().getProperty(
                            ConfigOptions.FATAL_ERRORS);

        waitingServer = deluxeQueues.getWaitingServer().orElse(null);
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
            deluxeQueues.getLogger().info("Target player timed out");
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
        deluxeQueues.getLogger().info("Attempting to connect player " + targetPlayer.toString());

        targetPlayer.getPlayer().createConnectionRequest(server).connect().thenAcceptAsync(result -> {
            targetPlayer.setConnecting(false);

            if(result.isSuccessful()) {
                return;
            }

            deluxeQueues.getLogger()
                    .info("Player " +
                                  targetPlayer.getPlayer().getUsername() + " failed to join "
                                  + queue.getServer().getServerInfo().getName()
                                  + ". Reason: "
                                  + PlainComponentSerializer.INSTANCE
                            .serialize(result.getReason().orElse(TextComponent.of("(None)"))));

            Component reason = result.getReason().orElse(TextComponent.empty());
            String reasonPlain = PlainComponentSerializer.INSTANCE.serialize(reason);
            ServerConnection currentServer = targetPlayer.getPlayer().getCurrentServer().orElse(null);

            boolean fatal = fatalErrors.stream().anyMatch(reasonPlain::contains);

		    if(fatal) {
                if(currentServer == null || currentServer.getServer().equals(waitingServer)) {
                    targetPlayer.getPlayer().disconnect(result.getReason().orElse(TextComponent.empty()));
                } else {
                    queue.removePlayer(targetPlayer, false);
                    deluxeQueues
                            .getCommandManager().sendMessage(targetPlayer.getPlayer(), MessageType.ERROR,
                                                             Messages.ERRORS__QUEUE_CANNOT_JOIN);
                    targetPlayer.getPlayer().sendMessage(reason);
                }
            }
        });

        targetPlayer.setConnecting(true);
    }

    private void handleQueue(ConcurrentLinkedQueue<QueuePlayer> q) {
        int position = 1;
        int disconnectTimeout = deluxeQueues.getSettingsHandler()
                .getSettingsManager().getProperty(ConfigOptions.DISCONNECT_TIMEOUT);

        for(Iterator i = q.iterator(); i.hasNext();) {
            QueuePlayer player = (QueuePlayer) i.next();
            Optional<ServerConnection> currentServer = player.getPlayer().getCurrentServer();
            boolean online = player.getPlayer().isActive();

            if(online || player.getLastSeen() == null) {
                player.setLastSeen(Instant.now());
            }

            if(online && currentServer.isPresent() && currentServer.get().getServer().equals(server)) {
                deluxeQueues.getLogger()
                    .info("Removing already connected player " + player.getPlayer().getUsername() + " from queue position #" + position + " in " + player.getQueueType());

                queue.removePlayer(player, true);
                continue;
            }

            if(!online && player.getLastSeen().isBefore(Instant.now().minusSeconds(disconnectTimeout))) {
                deluxeQueues.getLogger()
                    .info("Removing timed out player " + player.getPlayer().getUsername() + " from queue position #" + position + " in " + player.getQueueType());

                queue.removePlayer(player, false);
                continue;
            }

            deluxeQueues.getLogger()
                    .info("Player " + player.getPlayer().getUsername() + " is in queue position #" + position + " in " + player.getQueueType());

            if(targetPlayer == null && online) {
                targetPlayer = player;
                deluxeQueues.getLogger()
                    .info("Selecting player " + player.getPlayer().getUsername() + " is in queue position #" + position + " in " + player.getQueueType() + " as target.");
            }

            player.setPosition(position);
            queue.getNotifier().notifyPlayer(player);
            position++;
        }
    }
}
