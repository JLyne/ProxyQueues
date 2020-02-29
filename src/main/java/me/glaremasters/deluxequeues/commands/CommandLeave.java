package me.glaremasters.deluxequeues.commands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.CommandIssuer;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Dependency;
import co.aikar.commands.annotation.Description;
import co.aikar.commands.annotation.Subcommand;
import com.velocitypowered.api.proxy.Player;
import me.glaremasters.deluxequeues.messages.Messages;
import me.glaremasters.deluxequeues.queues.DeluxeQueue;
import me.glaremasters.deluxequeues.queues.QueueHandler;
import me.glaremasters.deluxequeues.utils.Constants;

import java.util.Optional;

@CommandAlias("%dq")
public class CommandLeave extends BaseCommand {

    @Dependency private QueueHandler queueHandler;

    @Subcommand("leave")
    @Description("{@@commands.leave-description}")
    @CommandPermission(Constants.BASE_PERM + "leave")
    public void execute(CommandIssuer sender) {
        Optional<DeluxeQueue> currentQueue = queueHandler.getCurrentQueue(sender.getIssuer());

        if(!currentQueue.isPresent()) {
            sender.sendError(Messages.ERRORS__PLAYER_NO_QUEUE);
            return;
        }

        queueHandler.clearPlayer(sender.getIssuer());
        getCurrentCommandIssuer().sendInfo(Messages.COMMANDS__LEAVE_SUCCESS,
                                           "%server%", currentQueue.get().getServer().getServerInfo().getName());
    }
}
