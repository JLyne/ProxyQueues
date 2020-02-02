package me.glaremasters.deluxequeues.queues;

import net.md_5.bungee.api.connection.ProxiedPlayer;

public class QueuePlayer {

    private ProxiedPlayer player;
    private boolean readyToMove;

    public QueuePlayer(ProxiedPlayer player, boolean readyToMove) {
        this.player = player;
        this.readyToMove = readyToMove;
    }

    public ProxiedPlayer getPlayer() {
        return player;
    }

    public boolean isReadyToMove() {
        return readyToMove;
    }

    public void setReadyToMove(boolean readyToMove) {
        this.readyToMove = readyToMove;
    }

}