package uk.co.notnull.proxyqueues.commands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.CommandIssuer;
import co.aikar.commands.MessageType;
import co.aikar.commands.annotation.*;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import uk.co.notnull.proxyqueues.ProxyQueues;
import uk.co.notnull.proxyqueues.messages.Messages;
import uk.co.notnull.proxyqueues.queues.ProxyQueue;
import uk.co.notnull.proxyqueues.queues.QueueHandler;
import uk.co.notnull.proxyqueues.utils.Constants;

@CommandAlias("%dq")
public class CommandClear extends BaseCommand {

    @Dependency private QueueHandler queueHandler;

    @Subcommand("clear")
    @Description("{@@commands.clear-description}")
    @CommandPermission(Constants.BASE_PERM + "clear")
    @CommandCompletion("@servers")
    public void server(CommandIssuer sender, RegisteredServer server) {
        ProxyQueue queue = queueHandler.getQueue(server);
        ProxyQueues proxyQueues = ProxyQueues.getInstance();

        if(queue == null) {
            proxyQueues.sendMessage(sender, MessageType.ERROR, Messages.ERRORS__SERVER_NO_QUEUE,
                                    "{server}", server.getServerInfo().getName());
            return;
        }

        queue.clear();

        proxyQueues.sendMessage(sender, MessageType.INFO, Messages.COMMANDS__CLEAR_SUCCESS,
                                "{server}", server.getServerInfo().getName());
    }
}
