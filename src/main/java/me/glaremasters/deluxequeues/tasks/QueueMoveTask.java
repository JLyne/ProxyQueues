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
import java.util.List;
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

        String waitingServerName = deluxeQueues.getSettingsHandler().getSettingsManager().getProperty(ConfigOptions.WAITING_SERVER);
        waitingServer = deluxeQueues.getProxyServer().getServer(waitingServerName).orElse(null);
    }

    @Override
    public void run() {
        ConcurrentLinkedQueue<QueuePlayer> normalQueue = queue.getQueue();
        ConcurrentLinkedQueue<QueuePlayer> priorityQueue = queue.getPriorityQueue();
        ConcurrentLinkedQueue<QueuePlayer> staffQueue = queue.getStaffQueue();

        if(targetPlayer != null && (!targetPlayer.isConnecting() || !targetPlayer.getPlayer().isActive())) {
            deluxeQueues.getLogger().info("Target player no longer valid");
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
            deluxeQueues.getLogger().info("No player to connect or already connecting");
            return;
        }

        // Check if the max amount of players on the server are the max slots
        if (queue.isServerFull(targetPlayer.getQueueType())) {
            deluxeQueues.getLogger().info("Too many players in server");
            return;
        }

        deluxeQueues.getLogger().info("Attempting to connect player " + targetPlayer.toString());

        targetPlayer.getPlayer().createConnectionRequest(server).connect().thenAcceptAsync(result -> {
            targetPlayer.setConnecting(false);

            if(result.isSuccessful()) {
                deluxeQueues.getLogger().info("Player " +
                                                      targetPlayer.getPlayer().getUsername() + " connected to "
                                                      + queue.getServer().getServerInfo().getName());

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

            fatalErrors.forEach(r -> {
                if(reasonPlain.contains(r)) {
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
        });

        targetPlayer.setConnecting(true);
    }

    private void handleQueue(ConcurrentLinkedQueue<QueuePlayer> q) {
        int position = 1;

        for(QueuePlayer player : q) {
            player.setPosition(position);
            queue.notifyPlayer(player);

            if(targetPlayer == null && player.getPlayer().isActive()) {
                targetPlayer = player;
            }

            position++;
        }
    }
}
