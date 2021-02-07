package uk.co.notnull.proxyqueues.commands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.CommandIssuer;
import co.aikar.commands.annotation.*;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import uk.co.notnull.proxyqueues.ProxyQueues;
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
    public void execute(CommandIssuer sender, String serverName) {
        Optional<RegisteredServer> server = ProxyQueues.getInstance().getProxyServer().getServer(serverName);

        if(server.isEmpty()) {
            sender.sendError(Messages.ERRORS__SERVER_UNKNOWN, "{server}", serverName);
            return;
        }

        ProxyQueue queue = queueHandler.getQueue(server.get());

        if(queue == null || !queue.isActive()) {
            sender.sendError(Messages.ERRORS__SERVER_NO_QUEUE, "{server}", serverName);
            return;
        }

        Optional<ServerConnection> currentServer = ((Player) sender.getIssuer()).getCurrentServer();

        if(currentServer.isPresent() && currentServer.get().getServer().equals(server.get())) {
            sender.sendError(Messages.ERRORS__PLAYER_SAME_SERVER, "{server}", serverName);
            return;
        }

        queueHandler.clearPlayer((Player) sender.getIssuer());
        queue.addPlayer(sender.getIssuer());
    }
}
