package uk.co.notnull.proxyqueues;

import com.velocitypowered.api.event.PostOrder;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import uk.co.notnull.proxydiscord.api.ProxyDiscord;
import uk.co.notnull.proxydiscord.api.VerificationResult;
import uk.co.notnull.proxydiscord.api.events.PlayerVerifyStateChangeEvent;
import uk.co.notnull.proxydiscord.api.manager.VerificationManager;
import uk.co.notnull.proxyqueues.events.PlayerQueueEvent;

import java.util.Optional;

public class ProxyDiscordHandler {
	private final ProxyQueues plugin;
	private static VerificationManager verificationManager;

	public ProxyDiscordHandler(ProxyQueues plugin) {
		this.plugin = plugin;

		ProxyDiscord proxyDiscord = (ProxyDiscord) plugin.getProxyServer().getPluginManager()
				.getPlugin("proxydiscord").get().getInstance().get();
		verificationManager = proxyDiscord.getVerificationManager();

		plugin.getProxyServer().getEventManager().register(plugin, this);
	}

	@Subscribe
	public void onPlayerJoinQueue(PlayerQueueEvent event) {
		if(event.isCancelled()) {
			return;
		}

        if(verificationManager.isPublicServer(event.getServer())) {
            return;
        }

        VerificationResult result = verificationManager.checkVerificationStatus(event.getPlayer());

        if(result.isVerified()) {
            Optional<ServerConnection> currentServer = event.getPlayer().getCurrentServer();

            if(currentServer.isPresent() && verificationManager.isPublicServer(currentServer.get().getServer())) {
                if(plugin.getWaitingServer().isPresent()) {
                    event.getPlayer().createConnectionRequest(plugin.getWaitingServer().get()).fireAndForget();
                }
            }

            return;
        }

		event.setCancelled(true);

        RegisteredServer linkingServer = verificationManager.getLinkingServer();
		RegisteredServer currentServer = event.getPlayer().getCurrentServer().map(ServerConnection::getServer)
				.orElse(null);

        if(linkingServer != null && (currentServer == null || !currentServer.equals(linkingServer))) {
        	event.getPlayer().createConnectionRequest(linkingServer).fireAndForget();
		}

        switch(result) {
            case NOT_LINKED:
                event.setReason(Messages.get("errors.discord-not-linked"));
                break;
            case LINKED_NOT_VERIFIED:
                event.setReason(Messages.get("errors.discord-not-verified"));
                break;
            default:
                event.setReason("An error has occurred.");
        }
	}

	@Subscribe(order = PostOrder.NORMAL)
	public void onPlayerVerifyStateChange(PlayerVerifyStateChangeEvent e) {
    	//Remove player from any queue
        if(!e.getState().isVerified()) {
        	plugin.getQueueHandler().clearPlayer(e.getPlayer());
		}
	}
}
