package uk.co.notnull.proxyqueues.commands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.CommandIssuer;
import co.aikar.commands.annotation.*;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
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
    public void execute(CommandIssuer sender, String target) {
        LuckPerms luckPermsApi = LuckPermsProvider.get();

        luckPermsApi.getUserManager().lookupUniqueId(target).thenAccept((UUID uuid) -> {
            if(uuid == null) {
                sender.sendError(Messages.ERRORS__TARGET_UNKNOWN, "%player%", target);
                return;
            }

            Optional<ProxyQueue> currentQueue = queueHandler.getCurrentQueue(uuid);

            if(currentQueue.isEmpty()) {
                sender.sendError(Messages.ERRORS__TARGET_NO_QUEUE, "%player%", target);
                return;
            }

            queueHandler.kickPlayer(uuid);
            sender.sendInfo(Messages.COMMANDS__KICK_SUCCESS,
                        "%player%", target,
                        "%server%", currentQueue.get().getServer().getServerInfo().getName());
        });
    }
}
