package uk.co.notnull.proxyqueues.commands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.CommandIssuer;
import co.aikar.commands.annotation.*;
import co.aikar.commands.velocity.contexts.OnlinePlayer;
import uk.co.notnull.proxyqueues.messages.Messages;
import uk.co.notnull.proxyqueues.queues.ProxyQueue;
import uk.co.notnull.proxyqueues.queues.QueueHandler;
import uk.co.notnull.proxyqueues.utils.Constants;

import java.util.Optional;

@CommandAlias("%dq")
public class CommandKick extends BaseCommand {

    @Dependency private QueueHandler queueHandler;

    @Subcommand("kick")
    @Description("{@@commands.kick-description}")
    @CommandPermission(Constants.BASE_PERM + "kick")
    @CommandCompletion("@players")
    public void execute(CommandIssuer sender, OnlinePlayer player) {
        Optional<ProxyQueue> currentQueue = queueHandler.getCurrentQueue(player.getPlayer());

        if(!currentQueue.isPresent()) {
            sender.sendError(Messages.ERRORS__TARGET_NO_QUEUE, "%player%", player.getPlayer().getUsername());
            return;
        }

        queueHandler.kickPlayer(player.getPlayer());
        sender.sendInfo(Messages.COMMANDS__KICK_SUCCESS,
                        "%player%", player.getPlayer().getUsername(),
                        "%server%", currentQueue.get().getServer().getServerInfo().getName());
    }
}
