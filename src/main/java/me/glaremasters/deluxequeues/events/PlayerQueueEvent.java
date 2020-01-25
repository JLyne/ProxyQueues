package me.glaremasters.deluxequeues.events;
;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.server.ServerInfo;

public class PlayerQueueEvent {
	private final Player player;
	private final ServerInfo server;
	private String reason = null;
	private boolean cancelled = false;

	public PlayerQueueEvent(Player player, ServerInfo server) {
		this.player = player;
		this.server = server;
	}

	public Player getPlayer() {
		return player;
	}

	public ServerInfo getServer() {
		return server;
	}

	public String getReason() {
		return reason;
	}

	public boolean isCancelled() {
		return cancelled;
	}

	public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }

    public void setReason(String reason) {
		this.reason = reason;
	}
}
