/*
 * ProxyQueues, a Velocity queueing solution
 *
 * Copyright (c) 2021 James Lyne
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package uk.co.notnull.proxyqueues;

import cloud.commandframework.CommandManager;
import cloud.commandframework.annotations.Argument;
import cloud.commandframework.annotations.CommandDescription;
import cloud.commandframework.annotations.CommandMethod;
import cloud.commandframework.annotations.CommandPermission;
import cloud.commandframework.annotations.specifier.Greedy;
import cloud.commandframework.minecraft.extras.MinecraftHelp;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import uk.co.notnull.proxyqueues.api.MessageType;
import uk.co.notnull.proxyqueues.api.QueueType;
import uk.co.notnull.proxyqueues.api.queues.ProxyQueue;
import uk.co.notnull.proxyqueues.api.queues.QueueHandler;
import uk.co.notnull.proxyqueues.api.queues.QueuePlayer;
import uk.co.notnull.proxyqueues.configuration.sections.ConfigOptions;
import uk.co.notnull.proxyqueues.queues.QueueHandlerImpl;
import uk.co.notnull.proxyqueues.utils.Constants;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static java.util.Map.entry;

public class Commands {
	private final ProxyQueuesImpl plugin;
	private final QueueHandler queueHandler;
    private final MinecraftHelp<CommandSource> minecraftHelp;

    public Commands(ProxyQueuesImpl plugin, CommandManager<CommandSource> commandManager) {
		this.plugin = plugin;
		this.queueHandler = plugin.getQueueHandler();

        this.minecraftHelp = new MinecraftHelp<>("/queue", p -> p, commandManager);
	}

    @CommandMethod("queue help [query]")
    private void commandHelp(
            final CommandSource sender,
            final @Argument("query") @Greedy String query
    ) {
        this.minecraftHelp.queryCommands(query == null ? "" : query, sender);
    }

	@CommandMethod("queue clear <server>")
    @CommandDescription("Clear the queue for the specified server, removing all players currently in the queue")
    @CommandPermission(Constants.BASE_PERM + "clear")
    public void clear(CommandSource sender, @Argument("server") RegisteredServer server) {
        ProxyQueue queue = queueHandler.getQueue(server);
        ProxyQueuesImpl proxyQueues = ProxyQueuesImpl.getInstance();

        if(queue == null) {
            proxyQueues.sendMessage(sender, MessageType.ERROR, "errors.server-no-queue",
                                    Map.of("{server}", server.getServerInfo().getName()));
            return;
        }

        queue.clear();

        proxyQueues.sendMessage(sender, MessageType.INFO, "commands.clear-success",
                                Map.of("{server}", server.getServerInfo().getName()));
    }

    @CommandMethod("queue info server <server>")
    @CommandDescription("Shows information about the specified server's queue")
    @CommandPermission(Constants.BASE_PERM + "info")
    public void serverInfo(CommandSource sender, @Argument("server") RegisteredServer server) {
        ProxyQueue queue = queueHandler.getQueue(server);
        ProxyQueuesImpl proxyQueues = ProxyQueuesImpl.getInstance();

        if(queue == null) {
            proxyQueues.sendMessage(sender, MessageType.ERROR, "errors.server-no-queue",
                                    Map.of("{server}", server.getServerInfo().getName()));
            return;
        }

        int playersRequired = queue.getPlayersRequired(),
            normalMax = queue.getMaxSlots(QueueType.NORMAL),
            priorityMax = queue.getMaxSlots(QueueType.PRIORITY),
            staffMax = queue.getMaxSlots(QueueType.STAFF),
            normalSize = queue.getQueueSize(QueueType.NORMAL),
            prioritySize = queue.getQueueSize(QueueType.PRIORITY),
            staffSize = queue.getQueueSize(QueueType.STAFF),
            connectedSize = queue.getConnectedCount(),
            priorityConnectedSize = queue.getConnectedCount(QueueType.PRIORITY),
            staffConnectedSize = queue.getConnectedCount(QueueType.STAFF);

        normalSize += prioritySize;
        normalSize += staffSize;

        QueuePlayer[] normalPlayers = queue.getTopPlayers(QueueType.NORMAL, 3);
        QueuePlayer[] priorityPlayers = queue.getTopPlayers(QueueType.PRIORITY, 3);
        QueuePlayer[] staffPlayers = queue.getTopPlayers(QueueType.STAFF, 3);

        proxyQueues.sendMessage(sender, MessageType.INFO, "commands.info-server-response", Map.ofEntries(
                entry("{server}", server.getServerInfo().getName()),
                entry("{size}", String.valueOf(normalSize)),
                entry("{prioritySize}", String.valueOf(prioritySize)),
                entry("{staffSize}", String.valueOf(staffSize)),
                entry("{connectedSize}", String.valueOf(connectedSize)),
                entry("{priorityConnectedSize}", String.valueOf(priorityConnectedSize)),
                entry("{staffConnectedSize}", String.valueOf(staffConnectedSize)),
                entry("{required}", String.valueOf(playersRequired)),
                entry("{max}", String.valueOf(normalMax)),
                entry("{priorityMax}", String.valueOf(priorityMax)),
                entry("{globalMax}", String.valueOf(staffMax)),
                entry("{staffFirst}", staffPlayers[0] != null ? staffPlayers[0].getPlayer().getUsername() : "n/a"),
                entry("{staffSecond}", staffPlayers[1] != null ? staffPlayers[1].getPlayer().getUsername() : "n/a"),
                entry("{staffThird}", staffPlayers[2] != null ? staffPlayers[2].getPlayer().getUsername() : "n/a"),
                entry("{priorityFirst}",
                      priorityPlayers[0] != null ? priorityPlayers[0].getPlayer().getUsername() : "n/a"),
                entry("{prioritySecond}",
                      priorityPlayers[1] != null ? priorityPlayers[1].getPlayer().getUsername() : "n/a"),
                entry("{priorityThird}",
                      priorityPlayers[2] != null ? priorityPlayers[2].getPlayer().getUsername() : "n/a"),
                entry("{first}", normalPlayers[0] != null ? normalPlayers[0].getPlayer().getUsername() : "n/a"),
                entry("{second}", normalPlayers[1] != null ? normalPlayers[1].getPlayer().getUsername() : "n/a"),
                entry("{third}", normalPlayers[2] != null ? normalPlayers[2].getPlayer().getUsername() : "n/a")));
    }

    @CommandMethod("queue info player <player>")
    @CommandDescription("Shows information about the specified player's queue status")
    @CommandPermission(Constants.BASE_PERM + "info")
    public void playerInfo(CommandSource sender, @Argument("player") Player player) {
        ProxyQueuesImpl proxyQueues = ProxyQueuesImpl.getInstance();
        UUID uuid = player.getUniqueId();

        ProxyQueue queue = queueHandler.getCurrentQueue(uuid).orElse(null);

        if(queue == null) {
            proxyQueues.sendMessage(sender, MessageType.ERROR, "errors.target-no-queue",
                                    Map.of("{player}", player.getUsername()));
            return;
        }

        QueuePlayer queuePlayer = queue.getQueuePlayer(uuid).get();
        String status;
        long queuedTime = queuePlayer.getQueuedTime();

        if(queuePlayer.getPlayer().isActive()) {
            status = Messages.get("commands.info-status-online");
        } else {
            int disconnectTimeout = ProxyQueuesImpl.getInstance().getSettingsHandler()
                    .getSettingsManager().getProperty(ConfigOptions.DISCONNECT_TIMEOUT);

            long lastSeenTime = queuePlayer.getLastSeen().until(Instant.now(), ChronoUnit.SECONDS);
            long remainingTime = Math.max(0, Instant.now().minusSeconds(disconnectTimeout)
                    .until(queuePlayer.getLastSeen(), ChronoUnit.SECONDS));

            status = Messages.get("commands.info-status-offline",
                                  Map.of("{lastseen}", lastSeenTime + "s", "{remaining}", remainingTime + "s"));
        }

        proxyQueues.sendMessage(sender, MessageType.INFO, "commands.info-player-response",
                        Map.of("{player}", player.getUsername(),
                        "{server}", queue.getServer().getServerInfo().getName(),
                        "{type}", queuePlayer.getQueueType().toString(),
                        "{position}", Integer.toString(queuePlayer.getPosition()),
                        "{status}", status,
                        "{queuedTime}", queuedTime + "s"));
    }

    @CommandMethod("queue join <server>")
    @CommandDescription("Join the queue for a server")
    @CommandPermission(Constants.BASE_PERM + "join")
    public void join(CommandSource sender, @Argument("server") RegisteredServer server) {
        ProxyQueue queue = queueHandler.getQueue(server);
        ProxyQueuesImpl proxyQueues = ProxyQueuesImpl.getInstance();

        if(queue == null || !queue.isActive()) {
            proxyQueues.sendMessage(sender, MessageType.ERROR, "errors.server-no-queue",
                                    Map.of("{server}", server.getServerInfo().getName()));
            return;
        }

        Optional<ServerConnection> currentServer = ((Player) sender).getCurrentServer();

        if(currentServer.isPresent() && currentServer.get().getServer().equals(server)) {
            proxyQueues.sendMessage(sender, MessageType.ERROR, "errors.player-same-server",
                                    Map.of("{server}", server.getServerInfo().getName()));
            return;
        }

        queueHandler.clearPlayer((Player) sender);
        queue.addPlayer((Player) sender);
    }

    @CommandMethod("queue kick <player>")
    @CommandDescription("Kick the specified player from any queue they are in")
    @CommandPermission(Constants.BASE_PERM + "kick")
    public void kick(CommandSource sender, @Argument("player") Player player) {
        ProxyQueuesImpl proxyQueues = ProxyQueuesImpl.getInstance();
        UUID uuid = player.getUniqueId();

        Optional<ProxyQueue> currentQueue = queueHandler.getCurrentQueue(uuid);

        if(currentQueue.isEmpty()) {
            proxyQueues.sendMessage(sender, MessageType.ERROR, "errors.target-no-queue",
                                    Map.of("{player}", player.getUsername()));
            return;
        }

        queueHandler.kickPlayer(uuid);
        proxyQueues.sendMessage(sender, MessageType.INFO, "commands.kick-success",
                    Map.of("{player}", player.getUsername(),
                    "{server}", currentQueue.get().getServer().getServerInfo().getName()));
    }

 	@CommandMethod("queue leave")
    @CommandDescription("Leave the current queue you are in")
    @CommandPermission(Constants.BASE_PERM + "leave")
    public void leave(CommandSource sender) {
        ProxyQueuesImpl proxyQueues = ProxyQueuesImpl.getInstance();
        Player player = (Player) sender;
        Optional<ProxyQueue> currentQueue = queueHandler.getCurrentQueue(player);

        if(currentQueue.isEmpty()) {
            proxyQueues.sendMessage(sender, MessageType.ERROR, "errors.player-no-queue");
            return;
        }

        queueHandler.clearPlayer(player, false);
    }

    @CommandMethod("queue reload")
    @CommandDescription("Reload the configuration of the plugin")
    @CommandPermission(Constants.ADMIN_PERM)
    public void reload(CommandSource issuer) {
        plugin.getSettingsHandler().getSettingsManager().reload();
        plugin.setPlayerLimit(plugin.getSettingsHandler().getSettingsManager().getProperty(ConfigOptions.PLAYER_LIMIT));
        ((QueueHandlerImpl) queueHandler).updateQueues();
        plugin.sendMessage(issuer, MessageType.INFO, "commands.reload-success");
    }
}
