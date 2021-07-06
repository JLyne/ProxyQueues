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

package uk.co.notnull.proxyqueues.metrics;

import com.velocitypowered.api.proxy.server.RegisteredServer;
import de.sldk.mc.metrics.ServerMetric;
import io.prometheus.client.Gauge;
import uk.co.notnull.proxyqueues.ProxyQueuesImpl;
import uk.co.notnull.proxyqueues.api.QueueType;
import uk.co.notnull.proxyqueues.api.queues.ProxyQueue;

public class PlayersQueued extends ServerMetric {
	private static final Gauge playersQueued = Gauge.build()
            .name(prefix("players_queued"))
            .labelNames("queue_type", "server")
            .help("Number of players queued by server and queue type")
            .create();

    public PlayersQueued(ProxyQueuesImpl plugin) {
        super(plugin, playersQueued);
    }

    @Override
    public void collect(RegisteredServer server) {
        ProxyQueue queue = ((ProxyQueuesImpl) plugin).getQueueHandler().getQueue(server);

        if(queue != null) {
            playersQueued.labels("normal", server.getServerInfo().getName()).set(queue.getQueueSize(QueueType.NORMAL));
            playersQueued.labels("priority", server.getServerInfo().getName()).set(queue.getQueueSize(QueueType.PRIORITY));
            playersQueued.labels("staff", server.getServerInfo().getName()).set(queue.getQueueSize(QueueType.STAFF));
        }
    }
}
