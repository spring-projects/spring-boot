/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.rabbitmq.metrics;

import java.util.Collections;

import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.impl.MicrometerMetricsCollector;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.binder.MeterBinder;
import org.jspecify.annotations.Nullable;

import org.springframework.util.Assert;

/**
 * A {@link MeterBinder} for RabbitMQ Java Client metrics.
 *
 * @author Arnaud Cogoluègnes
 * @author Stephane Nicoll
 * @since 4.2.0
 */
public class RabbitMetrics implements MeterBinder {

	private final Iterable<Tag> tags;

	private final ConnectionFactory connectionFactory;

	/**
	 * Create a new meter binder recording the specified {@link ConnectionFactory}.
	 * @param connectionFactory the {@link ConnectionFactory} to instrument
	 * @param tags tags to apply to all recorded metrics
	 */
	public RabbitMetrics(ConnectionFactory connectionFactory, @Nullable Iterable<Tag> tags) {
		Assert.notNull(connectionFactory, "'connectionFactory' must not be null");
		this.connectionFactory = connectionFactory;
		this.tags = (tags != null) ? tags : Collections.emptyList();
	}

	@Override
	public void bindTo(MeterRegistry registry) {
		this.connectionFactory.setMetricsCollector(new MicrometerMetricsCollector(registry, "rabbitmq", this.tags));
	}

}
