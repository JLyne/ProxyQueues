package uk.co.notnull.proxyqueues.queues;
import com.velocitypowered.api.proxy.Player;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.TextComponent;
import uk.co.notnull.proxyqueues.QueueType;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

public class QueuePlayer {

    private final BossBar bossBar;
    private boolean connecting;
    private Player player;
    private int position = -1;
    private final QueueType queueType;

    private Instant lastConnectionAttempt;
    private Instant lastSeen;
    private Instant queued;

    public QueuePlayer(Player player, QueueType queueType) {
        this.player = player;
        this.connecting = false;
        this.queueType = queueType;
        this.lastConnectionAttempt = Instant.EPOCH;
        this.lastSeen = null;
        this.queued = Instant.now();

        this.bossBar = BossBar.of(TextComponent.of("Joining queue..."), 0, getBossBarColor(),
                                  BossBar.Overlay.PROGRESS);
        this.player.showBossBar(bossBar);
    }

    public void showBossBar() {
        getPlayer().showBossBar(getBossBar());
    }

    public void hideBossBar() {
        getPlayer().hideBossBar(getBossBar());
    }

    public Player getPlayer() {
        return player;
    }

    public void setPlayer(Player player) {
        this.player.hideBossBar(bossBar);
        this.player = player;
        this.player.showBossBar(bossBar);
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

    public long getQueuedTime() {
        return queued.until(Instant.now(), ChronoUnit.SECONDS);
    }

    public BossBar.Color getBossBarColor() {
        switch(queueType) {
            case PRIORITY:
                return BossBar.Color.GREEN;
            case STAFF:
                return BossBar.Color.BLUE;
            default:
                return BossBar.Color.PURPLE;
        }
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