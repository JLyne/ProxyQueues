package uk.co.notnull.proxyqueues.queues;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.util.bossbar.BossBar;
import com.velocitypowered.api.util.bossbar.BossBarColor;
import com.velocitypowered.api.util.bossbar.BossBarOverlay;
import uk.co.notnull.proxyqueues.ProxyQueues;
import uk.co.notnull.proxyqueues.QueueType;
import net.kyori.text.TextComponent;

import java.time.Instant;

public class QueuePlayer {

    private final BossBar bossBar;
    private boolean connecting;
    private Player player;
    private int position = -1;
    private final QueueType queueType;
    private Instant lastConnectionAttempt;
    private Instant lastSeen;

    public QueuePlayer(Player player, QueueType queueType) {
        this.player = player;
        this.connecting = false;
        this.queueType = queueType;
        this.lastConnectionAttempt = Instant.EPOCH;
        this.lastSeen = null;

        this.bossBar = ProxyQueues.getInstance().getProxyServer()
                .createBossBar(TextComponent.of("Joining queue..."), BossBarColor.PURPLE, BossBarOverlay.PROGRESS, 0);
        this.bossBar.setVisible(true);
        this.bossBar.addPlayer(player);
    }

    public Player getPlayer() {
        return player;
    }

    public void setPlayer(Player player) {
        bossBar.removeAllPlayers();
        this.player = player;
        bossBar.addPlayer(player);
    }

    public BossBar getBossBar() {
        return bossBar;
    }

    public boolean isConnecting() {
        return connecting;
    }

    public void setConnecting(boolean connecting) {
        this.connecting = connecting;

        if(connecting) {
            this.lastConnectionAttempt = Instant.now();
        }
    }

    public Instant getLastConnectionAttempt() {
        return lastConnectionAttempt;
    }

    public int getPosition() {
        return position;
    }

    public void setPosition(int position) {
        this.position = position;
    }

    public QueueType getQueueType() {
        return queueType;
    }

    public Instant getLastSeen() {
        return lastSeen;
    }

    public void setLastSeen(Instant lastSeen) {
        this.lastSeen = lastSeen;
    }

    @Override
    public String toString() {
        return "QueuePlayer{" +
                "player=" + player +
                ", queueType=" + queueType +
                ", position=" + position +
                ", connecting=" + connecting +
                ", lastSeen=" + lastSeen +
                '}';
    }
}