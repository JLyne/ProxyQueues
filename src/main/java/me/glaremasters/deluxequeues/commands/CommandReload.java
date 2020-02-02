package me.glaremasters.deluxequeues.commands;

import ch.jalu.configme.SettingsManager;
import co.aikar.commands.BaseCommand;
import co.aikar.commands.CommandIssuer;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Dependency;
import co.aikar.commands.annotation.Description;
import co.aikar.commands.annotation.Subcommand;
import me.glaremasters.deluxequeues.DeluxeQueues;
import me.glaremasters.deluxequeues.messages.Messages;
import me.glaremasters.deluxequeues.queues.QueueHandler;
import me.glaremasters.deluxequeues.utils.Constants;
import net.md_5.bungee.api.connection.ProxiedPlayer;

@CommandAlias("%dq")
public class CommandReload extends BaseCommand {

    @Dependency private SettingsManager settingsManager;
    @Dependency private DeluxeQueues deluxeQueues;

    @Subcommand("reload")
    @Description("{@@descriptions.reload}")
    @CommandPermission(Constants.ADMIN_PERM)
    public void execute(CommandIssuer issuer) {
        settingsManager.reload();
        deluxeQueues.startQueues();
        issuer.sendInfo(Messages.RELOAD__SUCCESS);
    }
}
