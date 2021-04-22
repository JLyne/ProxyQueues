package uk.co.notnull.proxyqueues.metrics;

import de.sldk.mc.core.MetricRegistry;
import uk.co.notnull.proxyqueues.ProxyQueues;

public class MetricsHandler {
	public MetricsHandler(ProxyQueues plugin) {
		PlayersQueued playersQueued = new PlayersQueued(plugin);
		MetricRegistry.getInstance().register(playersQueued);
		playersQueued.enable();
	}
}
