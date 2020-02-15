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

import java.util.List;

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

    public QueueMoveTask(DeluxeQueue queue, RegisteredServer server, DeluxeQueues deluxeQueues) {
        this.queue = queue;
        this.server = server;
        this.deluxeQueues = deluxeQueues;
        this.fatalErrors = deluxeQueues.getSettingsHandler().getSettingsManager().getProperty(
                            ConfigOptions.FATAL_JOIN_ERRORS);

        String waitingServerName = deluxeQueues.getSettingsHandler().getSettingsManager().getProperty(ConfigOptions.WAITING_SERVER);
        waitingServer = deluxeQueues.getProxyServer().getServer(waitingServerName).orElse(null);
    }

    @Override
    public void run() {
        // Make sure the queue isn't empty
        if (queue.getQueue().isEmpty()) {
            return;
        }

        // Persist the notification to the user
        queue.getQueue().forEach(queue::notifyPlayer);

        // Check if the max amount of players on the server are the max slots
        if (queue.getServer().getPlayersConnected().size() >= queue.getMaxSlots()) {
            return;
        }

        // Get the player next in line
        QueuePlayer player = queue.getQueue().getFirst();
        // Make sure the player exists
        if (player != null) {
            player.getPlayer().createConnectionRequest(server).connect().thenAcceptAsync(result -> {
                if(!result.isSuccessful()) {
                    Component reason = result.getReason().orElse(TextComponent.empty());
                    String reasonPlain = PlainComponentSerializer.INSTANCE.serialize(reason);
                    ServerConnection currentServer = player.getPlayer().getCurrentServer().orElse(null);

                    fatalErrors.forEach(r -> {
                        if(reasonPlain.contains(r)) {
                            if(currentServer == null || currentServer.getServer().equals(waitingServer)) {
                                player.getPlayer().disconnect(result.getReason().orElse(TextComponent.empty()));
                            } else {
                                queue.removePlayer(player);
                                deluxeQueues.getCommandManager().sendMessage(player.getPlayer(), MessageType.ERROR,
                                                                             Messages.QUEUES__CANNOT_JOIN);
                                player.getPlayer().sendMessage(reason);
                            }
                        }
                    });
                }
            });

            player.setReadyToMove(true);
        }
    }
}
