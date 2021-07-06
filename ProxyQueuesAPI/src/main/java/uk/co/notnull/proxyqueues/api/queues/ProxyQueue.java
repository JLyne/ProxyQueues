/*
 * ProxyDiscord, a Velocity queueing solution
 *
 * Copyright (c) 2021 James Lyne
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

package uk.co.notnull.proxyqueues.api.queues;

import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import uk.co.notnull.proxyqueues.api.QueueType;

import java.util.Optional;
import java.util.UUID;


@SuppressWarnings("unused")
public interface ProxyQueue {

    void addPlayer(Player player);

    /**
     * Add a player to a queue.
     * If the player is already in the queue, but disconnected from the proxy, their current queue position and type will be used
     * Otherwise the provided queueType will be used, and the player will be added to the back of the queue
     * @param player the player to add
     */
    void addPlayer(Player player, QueueType queueType);

    /**
     * Removes the player from the queue, and updates connected caches
     * @param player - The player
     * @param connected - Whether player has now connected to the queued server, for cache updates
     */
    void removePlayer(QueuePlayer player, boolean connected);

    /**
     * Removes the player from the queue, and updates connected caches
     * @param player - The player
     * @param connected - Whether player has now connected to the queued server, for cache updates
     */
    void removePlayer(Player player, boolean connected);

    /**
     * Removes the player from the queue, and updates connected caches
     * @param uuid - The uuid of the player
     * @param connected - Whether player has now connected to the queued server, for cache updates
     */
    void removePlayer(UUID uuid, boolean connected);

    void destroy();

    void clear();

    /**
     * Returns whether the player is in this queue
     * @param player - The player
     * @return Whether the player is queued
     */
    boolean isPlayerQueued(Player player);

    boolean isPlayerQueued(UUID uuid);

    Optional<QueuePlayer> getQueuePlayer(Player player, boolean strict);

    Optional<QueuePlayer> getQueuePlayer(UUID uuid);

    /**
     * Returns whether the queue is active, and that players trying to join should be added to it
     * @return added or not
     */
    boolean isActive();

    boolean isServerFull(QueueType queueType);

    int getMaxSlots(QueueType queueType);

    int getQueueSize(QueueType queueType);

    int getConnectedCount();

    int getConnectedCount(QueueType queueType);

    QueuePlayer[] getTopPlayers(QueueType queueType, int count);

    RegisteredServer getServer();

    int getPlayersRequired();

    void setDelayLength(int delayLength);

    void setPlayersRequired(int playersRequired);

    void setMaxSlots(int maxSlots);

    void setPriorityMaxSlots(int priorityMaxSlots);

    void setStaffMaxSlots(int staffMaxSlots);
}
