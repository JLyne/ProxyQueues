package uk.co.notnull.proxyqueues.commands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.CommandIssuer;
import co.aikar.commands.MessageType;
import co.aikar.commands.annotation.*;
import co.aikar.commands.velocity.contexts.OnlinePlayer;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import uk.co.notnull.proxyqueues.ProxyQueues;
import uk.co.notnull.proxyqueues.QueueType;
import uk.co.notnull.proxyqueues.configuration.sections.ConfigOptions;
import uk.co.notnull.proxyqueues.messages.Messages;
import uk.co.notnull.proxyqueues.queues.ProxyQueue;
import uk.co.notnull.proxyqueues.queues.QueueHandler;
import uk.co.notnull.proxyqueues.queues.QueuePlayer;
import uk.co.notnull.proxyqueues.utils.Constants;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@CommandAlias("%dq")
public class CommandInfo extends BaseCommand {

    @Dependency private QueueHandler queueHandler;

    @Subcommand("info server")
    @Description("{@@commands.info-description}")
    @CommandPermission(Constants.BASE_PERM + "info")
    @CommandCompletion("@servers")
    public void server(CommandIssuer sender, RegisteredServer server) {
        ProxyQueue queue = queueHandler.getQueue(server);

        if(queue == null) {
            sender.sendError(Messages.ERRORS__SERVER_NO_QUEUE, "{server}", server.getServerInfo().getName());
            return;
        }

        int playersRequired = queue.getPlayersRequired(),
            normalMax = queue.getMaxSlots(QueueType.NORMAL),
            priorityMax = queue.getMaxSlots(QueueType.PRIORITY),
            staffMax = queue.getMaxSlots(QueueType.STAFF),
            normalSize = queue.getQueueSize(QueueType.NORMAL),
            prioritySize = queue.getQueueSize(QueueType.PRIORITY),
            staffSize = queue.getQueueSize(QueueType.STAFF),
            connectedSize = queue.getConnectedCount(),
            priorityConnectedSize = queue.getConnectedCount(QueueType.PRIORITY),
            staffConnectedSize = queue.getConnectedCount(QueueType.STAFF);

        normalSize += prioritySize;
        normalSize += staffSize;

        QueuePlayer[] normalPlayers = queue.getTopPlayers(QueueType.NORMAL, 3);
        QueuePlayer[] priorityPlayers = queue.getTopPlayers(QueueType.PRIORITY, 3);
        QueuePlayer[] staffPlayers = queue.getTopPlayers(QueueType.STAFF, 3);

        sender.sendInfo(Messages.COMMANDS__INFO_SERVER_RESPONSE,
                        "{server}", server.getServerInfo().getName(),
                        "{size}", String.valueOf(normalSize),
                        "{prioritySize}", String.valueOf(prioritySize),
                        "{staffSize}", String.valueOf(staffSize),
                        "{connectedSize}", String.valueOf(connectedSize),
                        "{priorityConnectedSize}", String.valueOf(priorityConnectedSize),
                        "{staffConnectedSize}", String.valueOf(staffConnectedSize),
                        "{required}", String.valueOf(playersRequired),
                        "{max}", String.valueOf(normalMax),
                        "{priorityMax}", String.valueOf(priorityMax),
                        "{globalMax}", String.valueOf(staffMax),
                        "{staffFirst}", staffPlayers[0] != null ? staffPlayers[0].getPlayer().getUsername() : "n/a",
                        "{staffSecond}", staffPlayers[1] != null ? staffPlayers[1].getPlayer().getUsername() : "n/a",
                        "{staffThird}", staffPlayers[2] != null ? staffPlayers[2].getPlayer().getUsername() : "n/a",
                        "{priorityFirst}", priorityPlayers[0] != null ? priorityPlayers[0].getPlayer().getUsername() : "n/a",
                        "{prioritySecond}", priorityPlayers[1] != null ? priorityPlayers[1].getPlayer().getUsername() : "n/a",
                        "{priorityThird}", priorityPlayers[2] != null ? priorityPlayers[2].getPlayer().getUsername() : "n/a",
                        "{first}", normalPlayers[0] != null ? normalPlayers[0].getPlayer().getUsername() : "n/a",
                        "{second}", normalPlayers[1] != null ? normalPlayers[1].getPlayer().getUsername() : "n/a",
                        "{third}", normalPlayers[2] != null ? normalPlayers[2].getPlayer().getUsername() : "n/a");
    }

    @Subcommand("info player")
    @Description("{@@commands.info-description}")
    @CommandPermission(Constants.BASE_PERM + "info")
    @CommandCompletion("@players")
    public void player(CommandIssuer sender, OnlinePlayer target) {
        Player player = target.getPlayer();
        UUID uuid = player.getUniqueId();

        ProxyQueue queue = queueHandler.getCurrentQueue(uuid).orElse(null);

        if(queue == null) {
            sender.sendError(Messages.ERRORS__TARGET_NO_QUEUE, "{player}", player.getUsername());
            return;
        }

        QueuePlayer queuePlayer = queue.getQueuePlayer(uuid).get();
        String status;
        long queuedTime = queuePlayer.getQueuedTime();

        if(queuePlayer.getPlayer().isActive()) {
            status = ProxyQueues.getInstance().getCommandManager()
                                  .formatMessage(sender, MessageType.INFO, Messages.COMMANDS__INFO_STATUS_ONLINE);
        } else {
            int disconnectTimeout = ProxyQueues.getInstance().getSettingsHandler()
                    .getSettingsManager().getProperty(ConfigOptions.DISCONNECT_TIMEOUT);

            long lastSeenTime = queuePlayer.getLastSeen().until(Instant.now(), ChronoUnit.SECONDS);
            long remainingTime = Math.max(0, Instant.now().minusSeconds(disconnectTimeout)
                    .until(queuePlayer.getLastSeen(), ChronoUnit.SECONDS));

            status = ProxyQueues.getInstance().getCommandManager()
                                  .formatMessage(sender, MessageType.INFO, Messages.COMMANDS__INFO_STATUS_OFFLINE,
                                                 "{lastseen}", lastSeenTime + "s",
                                                 "{remaining}", remainingTime + "s"
                                                 );
        }

        sender.sendInfo(Messages.COMMANDS__INFO_PLAYER_RESPONSE,
                        "{player}", player.getUsername(),
                        "{server}", queue.getServer().getServerInfo().getName(),
                        "{type}", queuePlayer.getQueueType().toString(),
                        "{position}", Integer.toString(queuePlayer.getPosition()),
                        "{status}", status,
                        "{queuedTime}", queuedTime + "s");
    }
}
