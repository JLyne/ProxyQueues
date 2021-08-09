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
import io.prometheus.client.Collector;
import io.prometheus.client.GaugeMetricFamily;
import uk.co.notnull.proxyqueues.ProxyQueuesImpl;
import uk.co.notnull.proxyqueues.api.QueueType;
import uk.co.notnull.proxyqueues.api.queues.ProxyQueue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class PlayersQueued extends Collector {
    @Override
    public List<MetricFamilySamples> collect() {
        ProxyQueuesImpl plugin = ProxyQueuesImpl.getInstance();
        List<MetricFamilySamples> mfs = new ArrayList<>();
        GaugeMetricFamily labeledGauge = new GaugeMetricFamily("mc_players_queued",
                                                               "Number of players queued by server and queue type",
                                                               Arrays.asList("queue_type", "server"));

        for (RegisteredServer server : plugin.getProxyServer().getAllServers()) {
            ProxyQueue queue = plugin.getQueueHandler().getQueue(server);

            if(queue != null) {
                String serverName = server.getServerInfo().getName();
                labeledGauge.addMetric(Arrays.asList(serverName, "normal"), queue.getQueueSize(QueueType.NORMAL));
                labeledGauge.addMetric(Arrays.asList(serverName, "priority"), queue.getQueueSize(QueueType.PRIORITY));
                labeledGauge.addMetric(Arrays.asList(serverName, "staff"), queue.getQueueSize(QueueType.STAFF));
                mfs.add(labeledGauge);
            }
        }

        mfs.add(labeledGauge);

        return mfs;
    }
}
