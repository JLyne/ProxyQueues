package me.glaremasters.deluxequeues.commands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.CommandIssuer;
import co.aikar.commands.annotation.*;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import me.glaremasters.deluxequeues.DeluxeQueues;
import me.glaremasters.deluxequeues.messages.Messages;
import me.glaremasters.deluxequeues.queues.DeluxeQueue;
import me.glaremasters.deluxequeues.queues.QueueHandler;
import me.glaremasters.deluxequeues.queues.QueuePlayer;
import me.glaremasters.deluxequeues.utils.Constants;

import java.util.Optional;

@CommandAlias("%dq")
public class CommandInfo extends BaseCommand {

    @Dependency private QueueHandler queueHandler;

    @Subcommand("info")
    @Description("{@@commands.info-description}")
    @CommandPermission(Constants.BASE_PERM + "info")
    @CommandCompletion("*")
    public void execute(CommandIssuer sender, String serverName) {
        Optional<RegisteredServer> server = DeluxeQueues.getInstance().getProxyServer().getServer(serverName);

        if(!server.isPresent()) {
            sender.sendError(Messages.ERRORS__SERVER_UNKNOWN, "%server%", serverName);
            return;
        }

        DeluxeQueue queue = queueHandler.getQueue(server.get());

        if(queue == null) {
            sender.sendError(Messages.ERRORS__SERVER_NO_QUEUE, "%server%", serverName);
            return;
        }

        int playersRequired = queue.getPlayersRequired(),
            maxSlots = queue.getMaxSlots(),
            playerCount = queue.getQueueSize();

        Optional<QueuePlayer> first = queue.getPlayerAt(0);
        Optional<QueuePlayer> second = queue.getPlayerAt(1);
        Optional<QueuePlayer> third = queue.getPlayerAt(2);

        sender.sendInfo(Messages.COMMANDS__INFO_RESPONSE,
                        "%server%", server.get().getServerInfo().getName(),
                        "%size%", String.valueOf(playerCount),
                        "%required%", String.valueOf(playersRequired),
                        "%max%", String.valueOf(maxSlots),
                        "%first%", first.isPresent() ? first.get().getPlayer().getUsername() : "n/a",
                        "%second%", second.isPresent() ? second.get().getPlayer().getUsername() : "n/a",
                        "%third%", third.isPresent() ? third.get().getPlayer().getUsername() : "n/a");
    }
}
