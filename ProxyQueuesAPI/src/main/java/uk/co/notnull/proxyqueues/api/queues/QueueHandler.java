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
import org.jetbrains.annotations.NotNull;

import java.util.*;

@SuppressWarnings("unused")
public interface QueueHandler {

    /**
     * Create a new queue for a server
     * @param server The server the new queue is for
     * @return ProxyQueue - The queue
     */
    ProxyQueue createQueue(@NotNull RegisteredServer server, int requiredPlayers, int maxNormal, int maxPriority, int maxStaff);

    /**
     * Delete a queue if it exists
     * @param server The server to remove the queue for
     */
    void deleteQueue(@NotNull RegisteredServer server);

    /**
     * Get a queue from it's server
     * @param server the server to get the queue for
     * @return the queue
     */
     ProxyQueue getQueue(@NotNull RegisteredServer server);

    /**
     * Get a queue from it's server
     * @param player the server to get the queue from
     * @return the queue
     */
    Optional<ProxyQueue> getCurrentQueue(@NotNull Player player);

    Optional<ProxyQueue> getCurrentQueue(@NotNull UUID uuid);

    /**
     * Remove a player from all queues
     * @param player the player to remove
     */
    void clearPlayer(Player player);

    /**
     * Remove a player from all queues
     * @param uuid the UUID of the player to remove
     */
    void clearPlayer(UUID uuid);

    /**
     * Remove a player from all queues
     * @param player The player to remove
     */
    void clearPlayer(Player player, boolean silent);

    /**
     * Remove a player from all queues
     * @param uuid The UUID of the player to remove
     */
    void clearPlayer(UUID uuid, boolean silent);

    void kickPlayer(Player player);

    void kickPlayer(UUID uuid);
}
