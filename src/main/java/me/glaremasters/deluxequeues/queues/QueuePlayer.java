package me.glaremasters.deluxequeues.queues;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.util.bossbar.BossBar;
import com.velocitypowered.api.util.bossbar.BossBarColor;
import com.velocitypowered.api.util.bossbar.BossBarOverlay;
import me.glaremasters.deluxequeues.DeluxeQueues;
import net.kyori.text.TextComponent;

public class QueuePlayer {

    private final Player player;
    private final BossBar bossBar;
    private boolean readyToMove;

    public QueuePlayer(Player player, boolean readyToMove) {
        this.player = player;
        this.readyToMove = readyToMove;

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

    public boolean isReadyToMove() {
        return readyToMove;
    }

    public void setReadyToMove(boolean readyToMove) {
        this.readyToMove = readyToMove;
    }

}