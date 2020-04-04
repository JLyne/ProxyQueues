package me.glaremasters.deluxequeues.commands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.CommandIssuer;
import co.aikar.commands.annotation.*;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import me.glaremasters.deluxequeues.DeluxeQueues;
import me.glaremasters.deluxequeues.QueueType;
import me.glaremasters.deluxequeues.messages.Messages;
import me.glaremasters.deluxequeues.queues.DeluxeQueue;
import me.glaremasters.deluxequeues.queues.QueueHandler;
import me.glaremasters.deluxequeues.queues.QueuePlayer;
import me.glaremasters.deluxequeues.utils.Constants;

import java.util.Optional;

@CommandAlias("%dq")
public class CommandInfo extends BaseCommand {

    @Dependency private QueueHandler queueHandler;

    @Subcommand("info")
    @Description("{@@commands.info-description}")
    @CommandPermission(Constants.BASE_PERM + "info")
    @CommandCompletion("*")
    public void execute(CommandIssuer sender, String serverName) {
        Optional<RegisteredServer> server = DeluxeQueues.getInstance().getProxyServer().getServer(serverName);

        if(!server.isPresent()) {
            sender.sendError(Messages.ERRORS__SERVER_UNKNOWN, "%server%", serverName);
            return;
        }

        DeluxeQueue queue = queueHandler.getQueue(server.get());

        if(queue == null) {
            sender.sendError(Messages.ERRORS__SERVER_NO_QUEUE, "%server%", serverName);
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

        sender.sendInfo(Messages.COMMANDS__INFO_RESPONSE,
                        "%server%", server.get().getServerInfo().getName(),
                        "%size%", String.valueOf(normalSize),
                        "%prioritySize%", String.valueOf(prioritySize),
                        "%staffSize%", String.valueOf(staffSize),
                        "%connectedSize%", String.valueOf(connectedSize),
                        "%priorityConnectedSize%", String.valueOf(priorityConnectedSize),
                        "%staffConnectedSize%", String.valueOf(staffConnectedSize),
                        "%required%", String.valueOf(playersRequired),
                        "%max%", String.valueOf(normalMax),
                        "%priorityMax%", String.valueOf(priorityMax),
                        "%globalMax%", String.valueOf(staffMax),
                        "%staffFirst%", staffPlayers[0] != null ? staffPlayers[0].getPlayer().getUsername() : "n/a",
                        "%staffSecond%", staffPlayers[1] != null ? staffPlayers[1].getPlayer().getUsername() : "n/a",
                        "%staffThird%", staffPlayers[2] != null ? staffPlayers[2].getPlayer().getUsername() : "n/a",
                        "%priorityFirst%", priorityPlayers[0] != null ? priorityPlayers[0].getPlayer().getUsername() : "n/a",
                        "%prioritySecond%", priorityPlayers[1] != null ? priorityPlayers[1].getPlayer().getUsername() : "n/a",
                        "%priorityThird%", priorityPlayers[2] != null ? priorityPlayers[2].getPlayer().getUsername() : "n/a",
                        "%first%", normalPlayers[0] != null ? normalPlayers[0].getPlayer().getUsername() : "n/a",
                        "%second%", normalPlayers[1] != null ? normalPlayers[1].getPlayer().getUsername() : "n/a",
                        "%third%", normalPlayers[2] != null ? normalPlayers[2].getPlayer().getUsername() : "n/a");
    }
}
