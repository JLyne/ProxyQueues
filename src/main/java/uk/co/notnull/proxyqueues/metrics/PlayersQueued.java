package uk.co.notnull.proxyqueues.metrics;

import com.velocitypowered.api.proxy.server.RegisteredServer;
import de.sldk.mc.metrics.ServerMetric;
import io.prometheus.client.Gauge;
import uk.co.notnull.proxyqueues.ProxyQueues;
import uk.co.notnull.proxyqueues.QueueType;
import uk.co.notnull.proxyqueues.queues.ProxyQueue;

public class PlayersQueued extends ServerMetric {
	private static final Gauge playersQueued = Gauge.build()
            .name(prefix("players_queued"))
            .labelNames("queue_type", "server")
            .help("Number of players queued by server and queue type")
            .create();

    public PlayersQueued(ProxyQueues plugin) {
        super(plugin, playersQueued);
    }

    @Override
    public void collect(RegisteredServer server) {
        ProxyQueue queue = ((ProxyQueues) plugin).getQueueHandler().getQueue(server);

        ((ProxyQueues) plugin).getLogger().info("Queue status for " + server.getServerInfo().getName());

        if(queue != null) {
            ((ProxyQueues) plugin).getLogger().info("Queue exists " + server.getServerInfo().getName());
            playersQueued.labels("normal", server.getServerInfo().getName()).set(queue.getQueueSize(QueueType.NORMAL));
            playersQueued.labels("priority", server.getServerInfo().getName()).set(queue.getQueueSize(QueueType.PRIORITY));
            playersQueued.labels("staff", server.getServerInfo().getName()).set(queue.getQueueSize(QueueType.STAFF));
        }
    }
}
