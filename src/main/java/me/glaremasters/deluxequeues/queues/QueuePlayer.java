package me.glaremasters.deluxequeues.queues;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.util.bossbar.BossBar;
import com.velocitypowered.api.util.bossbar.BossBarColor;
import com.velocitypowered.api.util.bossbar.BossBarOverlay;
import me.glaremasters.deluxequeues.DeluxeQueues;
import me.glaremasters.deluxequeues.QueueType;
import net.kyori.text.TextComponent;

import java.time.Instant;

public class QueuePlayer {

    private final Player player;
    private final BossBar bossBar;
    private boolean connecting;
    private int position;
    private QueueType queueType;
    private Instant lastConnectionAttempt;

    public QueuePlayer(Player player, QueueType queueType) {
        this.player = player;
        this.connecting = false;
        this.queueType = queueType;
        this.lastConnectionAttempt = Instant.EPOCH;

        this.bossBar = DeluxeQueues.getInstance().getProxyServer()
                .createBossBar(TextComponent.of("Joining queue..."), BossBarColor.PURPLE, BossBarOverlay.PROGRESS, 0);
        this.bossBar.setVisible(false);
        this.bossBar.addPlayer(player);
    }

    public Player getPlayer() {
        return player;
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

    @Override
    public String toString() {
        return "QueuePlayer{" +
                "player=" + player +
                ", queueType=" + queueType +
                ", position=" + position +
                ", connecting=" + connecting +
                '}';
    }
}