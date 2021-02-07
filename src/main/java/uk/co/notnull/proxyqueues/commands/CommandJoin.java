package uk.co.notnull.proxyqueues.commands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.CommandIssuer;
import co.aikar.commands.annotation.*;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import uk.co.notnull.proxyqueues.messages.Messages;
import uk.co.notnull.proxyqueues.queues.ProxyQueue;
import uk.co.notnull.proxyqueues.queues.QueueHandler;
import uk.co.notnull.proxyqueues.utils.Constants;

import java.util.Optional;

@CommandAlias("%dq")
public class CommandJoin extends BaseCommand {

    @Dependency private QueueHandler queueHandler;

    @Subcommand("join")
    @Description("{@@commands.join-description}")
    @CommandPermission(Constants.BASE_PERM + "join")
    @CommandCompletion("@servers")
    public void execute(CommandIssuer sender, RegisteredServer server) {
        ProxyQueue queue = queueHandler.getQueue(server);

        if(queue == null || !queue.isActive()) {
            sender.sendError(Messages.ERRORS__SERVER_NO_QUEUE, "{server}", server.getServerInfo().getName());
            return;
        }

        Optional<ServerConnection> currentServer = ((Player) sender.getIssuer()).getCurrentServer();

        if(currentServer.isPresent() && currentServer.get().getServer().equals(server)) {
            sender.sendError(Messages.ERRORS__PLAYER_SAME_SERVER, "{server}", server.getServerInfo().getName());
            return;
        }

        queueHandler.clearPlayer((Player) sender.getIssuer());
        queue.addPlayer(sender.getIssuer());
    }
}
