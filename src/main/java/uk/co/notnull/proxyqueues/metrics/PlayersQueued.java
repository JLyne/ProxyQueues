/*
 * ProxyQueues, a Velocity queueing solution
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

package uk.co.notnull.proxyqueues.metrics;

import com.velocitypowered.api.proxy.server.RegisteredServer;
import de.sldk.mc.metrics.AbstractMetric;
import io.prometheus.metrics.core.metrics.GaugeWithCallback;

import uk.co.notnull.proxyqueues.ProxyQueuesImpl;
import uk.co.notnull.proxyqueues.api.ProxyQueues;
import uk.co.notnull.proxyqueues.api.QueueType;
import uk.co.notnull.proxyqueues.api.queues.ProxyQueue;

public class PlayersQueued extends AbstractMetric {
    private static final GaugeWithCallback playersQueued = GaugeWithCallback.builder()
            .name(prefix("players_queued"))
            .help("Number of players queued by server and queue type")
            .labelNames("queue_type", "server")
            .callback(callback -> {
                ProxyQueuesImpl plugin = ProxyQueuesImpl.getInstance();

                for (RegisteredServer server : plugin.getProxyServer().getAllServers()) {
                    ProxyQueue queue = plugin.getQueueHandler().getQueue(server);

                    if(queue != null) {
                        String serverName = server.getServerInfo().getName();

                        callback.call(queue.getQueueSize(QueueType.NORMAL), serverName, "normal");
                        callback.call(queue.getQueueSize(QueueType.PRIORITY), serverName, "priority");
                        callback.call(queue.getQueueSize(QueueType.STAFF), serverName, "staff");
                    }
                }
            })
            .build();

    public PlayersQueued(ProxyQueues plugin) {
        super(plugin, playersQueued);
    }

    protected void initialValue() {
        playersQueued.collect();
    }
}
