package me.glaremasters.deluxequeues.events;

import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Cancellable;
import net.md_5.bungee.api.plugin.Event;

public class PlayerQueueEvent extends Event implements Cancellable {
	private final ProxiedPlayer player;
	private final ServerInfo server;
	private String reason = null;
	private boolean cancelled = false;

	public PlayerQueueEvent(ProxiedPlayer player, ServerInfo server) {
		this.player = player;
		this.server = server;
	}

	public ProxiedPlayer getPlayer() {
		return player;
	}

	public ServerInfo getServer() {
		return server;
	}

	public String getReason() {
		return reason;
	}

	@Override
	public boolean isCancelled() {
		return cancelled;
	}

	@Override
	public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }

    public void setReason(String reason) {
		this.reason = reason;
	}
}
