package uk.co.notnull.proxyqueues.commands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.CommandIssuer;
import co.aikar.commands.MessageType;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Dependency;
import co.aikar.commands.annotation.Description;
import co.aikar.commands.annotation.Subcommand;
import com.velocitypowered.api.proxy.Player;
import uk.co.notnull.proxyqueues.ProxyQueues;
import uk.co.notnull.proxyqueues.messages.Messages;
import uk.co.notnull.proxyqueues.queues.ProxyQueue;
import uk.co.notnull.proxyqueues.queues.QueueHandler;
import uk.co.notnull.proxyqueues.utils.Constants;

import java.util.Optional;

@CommandAlias("%dq")
public class CommandLeave extends BaseCommand {

    @Dependency private QueueHandler queueHandler;

    @Subcommand("leave")
    @Description("{@@commands.leave-description}")
    @CommandPermission(Constants.BASE_PERM + "leave")
    public void execute(CommandIssuer sender) {
        ProxyQueues proxyQueues = ProxyQueues.getInstance();
        Player player = sender.getIssuer();
        Optional<ProxyQueue> currentQueue = queueHandler.getCurrentQueue(player);

        if(currentQueue.isEmpty()) {
            proxyQueues.sendMessage(sender, MessageType.ERROR, Messages.ERRORS__PLAYER_NO_QUEUE);
            return;
        }

        queueHandler.clearPlayer(player, false);
    }
}
