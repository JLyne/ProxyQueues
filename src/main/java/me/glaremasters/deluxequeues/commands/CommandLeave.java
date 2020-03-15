package me.glaremasters.deluxequeues.commands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.CommandIssuer;
import co.aikar.commands.MessageType;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Dependency;
import co.aikar.commands.annotation.Description;
import co.aikar.commands.annotation.Subcommand;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import me.glaremasters.deluxequeues.DeluxeQueues;
import me.glaremasters.deluxequeues.configuration.SettingsHandler;
import me.glaremasters.deluxequeues.configuration.sections.ConfigOptions;
import me.glaremasters.deluxequeues.messages.Messages;
import me.glaremasters.deluxequeues.queues.DeluxeQueue;
import me.glaremasters.deluxequeues.queues.QueueHandler;
import me.glaremasters.deluxequeues.utils.Constants;
import net.kyori.text.TextComponent;
import ninja.leaping.configurate.objectmapping.Setting;

import java.util.Optional;

@CommandAlias("%dq")
public class CommandLeave extends BaseCommand {

    @Dependency private QueueHandler queueHandler;

    @Subcommand("leave")
    @Description("{@@commands.leave-description}")
    @CommandPermission(Constants.BASE_PERM + "leave")
    public void execute(CommandIssuer sender) {
        Player player = sender.getIssuer();
        Optional<DeluxeQueue> currentQueue = queueHandler.getCurrentQueue(player);

        if(!currentQueue.isPresent()) {
            sender.sendError(Messages.ERRORS__PLAYER_NO_QUEUE);
            return;
        }

        queueHandler.clearPlayer(player, false);
    }
}
