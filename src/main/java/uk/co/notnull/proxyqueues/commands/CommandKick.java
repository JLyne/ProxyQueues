package uk.co.notnull.proxyqueues.commands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.CommandIssuer;
import co.aikar.commands.MessageType;
import co.aikar.commands.annotation.*;
import co.aikar.commands.velocity.contexts.OnlinePlayer;
import com.velocitypowered.api.proxy.Player;
import uk.co.notnull.proxyqueues.ProxyQueues;
import uk.co.notnull.proxyqueues.messages.Messages;
import uk.co.notnull.proxyqueues.queues.ProxyQueue;
import uk.co.notnull.proxyqueues.queues.QueueHandler;
import uk.co.notnull.proxyqueues.utils.Constants;

import java.util.Optional;
import java.util.UUID;

@CommandAlias("%dq")
public class CommandKick extends BaseCommand {

    @Dependency private QueueHandler queueHandler;

    @Subcommand("kick")
    @Description("{@@commands.kick-description}")
    @CommandPermission(Constants.BASE_PERM + "kick")
    @CommandCompletion("@players")
    public void execute(CommandIssuer sender, OnlinePlayer target) {
        ProxyQueues proxyQueues = ProxyQueues.getInstance();
        Player player = target.getPlayer();
        UUID uuid = player.getUniqueId();

        Optional<ProxyQueue> currentQueue = queueHandler.getCurrentQueue(uuid);

        if(currentQueue.isEmpty()) {
            proxyQueues.sendMessage(sender, MessageType.ERROR, Messages.ERRORS__TARGET_NO_QUEUE,
                                    "{player}", player.getUsername());
            return;
        }

        queueHandler.kickPlayer(uuid);
        proxyQueues.sendMessage(sender, MessageType.INFO, Messages.COMMANDS__KICK_SUCCESS,
                    "{player}", player.getUsername(),
                    "{server}", currentQueue.get().getServer().getServerInfo().getName());
    }
}
