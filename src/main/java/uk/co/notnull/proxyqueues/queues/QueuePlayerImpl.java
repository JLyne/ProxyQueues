/*
 * ProxyQueues, a Velocity queueing solution
 * Copyright (c) 2021 James Lyne
 *
 * Some portions of this file were taken from https://github.com/darbyjack/DeluxeQueues
 * These portions are Copyright (c) 2019 Glare
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

package uk.co.notnull.proxyqueues.queues;
import com.velocitypowered.api.proxy.Player;
import net.kyori.adventure.bossbar.BossBar;
import uk.co.notnull.proxyqueues.Messages;
import uk.co.notnull.proxyqueues.api.QueueType;
import uk.co.notnull.proxyqueues.api.queues.ProxyQueue;
import uk.co.notnull.proxyqueues.api.queues.QueuePlayer;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

public class QueuePlayerImpl implements QueuePlayer {

    private final BossBar bossBar;
    private boolean connecting;
    private Player player;
    private int position = -1;
    private final ProxyQueue queue;
    private final QueueType queueType;

    private Instant lastConnectionAttempt;
    private Instant lastSeen;
    private final Instant queued;

    public QueuePlayerImpl(Player player, ProxyQueue queue, QueueType queueType) {
        this.player = player;
        this.connecting = false;
        this.queue = queue;
        this.queueType = queueType;
        this.lastConnectionAttempt = Instant.EPOCH;
        this.lastSeen = null;
        this.queued = Instant.now();

        this.bossBar = BossBar.bossBar(Messages.getComponent("notify.joining"), 0, getBossBarColor(),
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
        if(queue.isPaused()) {
            return BossBar.Color.WHITE;
        }

        return switch (queueType) {
            case PRIORITY -> BossBar.Color.GREEN;
            case STAFF -> BossBar.Color.BLUE;
            default -> BossBar.Color.PURPLE;
        };
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