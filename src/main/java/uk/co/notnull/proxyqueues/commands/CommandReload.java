package uk.co.notnull.proxyqueues.commands;

import ch.jalu.configme.SettingsManager;
import co.aikar.commands.BaseCommand;
import co.aikar.commands.CommandIssuer;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Dependency;
import co.aikar.commands.annotation.Description;
import co.aikar.commands.annotation.Subcommand;
import uk.co.notnull.proxyqueues.ProxyQueues;
import uk.co.notnull.proxyqueues.messages.Messages;
import uk.co.notnull.proxyqueues.utils.Constants;

@CommandAlias("%dq")
public class CommandReload extends BaseCommand {

    @Dependency private SettingsManager settingsManager;
    @Dependency private ProxyQueues proxyQueues;

    @Subcommand("reload")
    @Description("{@@commands.reload-description}")
    @CommandPermission(Constants.ADMIN_PERM)
    public void execute(CommandIssuer issuer) {
        settingsManager.reload();
        proxyQueues.getQueueHandler().updateQueues();
        issuer.sendInfo(Messages.COMMANDS__RELOAD_SUCCESS);
    }
}
