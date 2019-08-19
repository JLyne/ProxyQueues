package me.glaremasters.deluxequeues.commands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Dependency;
import co.aikar.commands.annotation.Description;
import co.aikar.commands.annotation.Subcommand;
import me.glaremasters.deluxequeues.messages.Messages;
import me.glaremasters.deluxequeues.queues.QueueHandler;
import me.glaremasters.deluxequeues.utils.Constants;
import net.md_5.bungee.api.connection.ProxiedPlayer;

@CommandAlias("%dq")
public class CommandLeave extends BaseCommand {

    @Dependency private QueueHandler queueHandler;

    @Subcommand("leave")
    @Description("{@@descriptions.command-leave}")
    @CommandPermission(Constants.BASE_PERM + "leave")
    public void execute(ProxiedPlayer player) {
        queueHandler.clearPlayer(player);
        getCurrentCommandIssuer().sendInfo(Messages.QUEUES__LEFT);
    }
}
