package me.glaremasters.deluxequeues.queues;
import com.velocitypowered.api.proxy.Player;

public class QueuePlayer {

    private final Player player;
    private boolean readyToMove;

    public QueuePlayer(Player player, boolean readyToMove) {
        this.player = player;
        this.readyToMove = readyToMove;
    }

    public Player getPlayer() {
        return player;
    }

    public boolean isReadyToMove() {
        return readyToMove;
    }

    public void setReadyToMove(boolean readyToMove) {
        this.readyToMove = readyToMove;
    }

}