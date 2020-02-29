package me.glaremasters.deluxequeues.commands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.CommandIssuer;
import co.aikar.commands.annotation.*;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import me.glaremasters.deluxequeues.DeluxeQueues;
import me.glaremasters.deluxequeues.messages.Messages;
import me.glaremasters.deluxequeues.queues.DeluxeQueue;
import me.glaremasters.deluxequeues.queues.QueueHandler;
import me.glaremasters.deluxequeues.utils.Constants;

import java.util.Optional;

@CommandAlias("%dq")
public class CommandJoin extends BaseCommand {

    @Dependency private QueueHandler queueHandler;

    @Subcommand("join")
    @Description("{@@commands.join-description}")
    @CommandPermission(Constants.BASE_PERM + "join")
    public void execute(CommandIssuer sender, String serverName) {
        Optional<RegisteredServer> server = DeluxeQueues.getInstance().getProxyServer().getServer(serverName);

        if(!server.isPresent()) {
            sender.sendError(Messages.ERRORS__SERVER_UNKNOWN, "%server%", serverName);
            return;
        }

        DeluxeQueue queue = queueHandler.getQueue(server.get());

        if(queue == null || !queue.isActive()) {
            sender.sendError(Messages.ERRORS__SERVER_NO_QUEUE, "%server%", serverName);
            return;
        }

        Optional<ServerConnection> currentServer = ((Player) sender.getIssuer()).getCurrentServer();

        if(currentServer.isPresent() && currentServer.get().getServer().equals(server.get())) {
            sender.sendError(Messages.ERRORS__PLAYER_SAME_SERVER, "%server%", serverName);
            return;
        }

        queueHandler.clearPlayer(sender.getIssuer());
        queue.addPlayer(sender.getIssuer());
    }
}
