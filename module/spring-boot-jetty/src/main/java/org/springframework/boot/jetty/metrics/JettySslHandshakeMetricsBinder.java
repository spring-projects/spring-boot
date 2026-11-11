/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.jetty.metrics;

import java.util.Collections;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.binder.jetty.JettySslHandshakeMetrics;
import org.eclipse.jetty.server.Server;

/**
 * {@link AbstractJettyMetricsBinder} for {@link JettySslHandshakeMetrics}.
 *
 * @author Chris Bono
 * @since 4.0.0
 */
public class JettySslHandshakeMetricsBinder extends AbstractJettyMetricsBinder {

	private final MeterRegistry meterRegistry;

	private final Iterable<Tag> tags;

	public JettySslHandshakeMetricsBinder(MeterRegistry meterRegistry) {
		this(meterRegistry, Collections.emptyList());
	}

	public JettySslHandshakeMetricsBinder(MeterRegistry meterRegistry, Iterable<Tag> tags) {
		this.meterRegistry = meterRegistry;
		this.tags = tags;
	}

	@Override
	protected void bindMetrics(Server server) {
		JettySslHandshakeMetrics.addToAllConnectors(server, this.meterRegistry, this.tags);
	}

}
