/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.jetty.metrics;

import java.util.Collections;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.binder.jetty.JettyServerThreadPoolMetrics;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.util.thread.ThreadPool;

/**
 * {@link AbstractJettyMetricsBinder} for {@link JettyServerThreadPoolMetrics}.
 *
 * @author Andy Wilkinson
 * @since 4.0.0
 */
public class JettyServerThreadPoolMetricsBinder extends AbstractJettyMetricsBinder {

	private final MeterRegistry meterRegistry;

	private final Iterable<Tag> tags;

	public JettyServerThreadPoolMetricsBinder(MeterRegistry meterRegistry) {
		this(meterRegistry, Collections.emptyList());
	}

	public JettyServerThreadPoolMetricsBinder(MeterRegistry meterRegistry, Iterable<Tag> tags) {
		this.meterRegistry = meterRegistry;
		this.tags = tags;
	}

	@Override
	@SuppressWarnings("resource")
	protected void bindMetrics(Server server) {
		ThreadPool threadPool = server.getThreadPool();
		if (threadPool != null) {
			new JettyServerThreadPoolMetrics(threadPool, this.tags).bindTo(this.meterRegistry);
		}
	}

}
