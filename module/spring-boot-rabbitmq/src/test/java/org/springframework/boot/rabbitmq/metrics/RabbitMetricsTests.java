/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.rabbitmq.metrics;

import com.rabbitmq.client.ConnectionFactory;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link RabbitMetrics}.
 *
 * @author Stephane Nicoll
 */
class RabbitMetricsTests {

	@Test
	void connectionFactoryIsInstrumented() {
		ConnectionFactory connectionFactory = mock(ConnectionFactory.class);
		SimpleMeterRegistry registry = new SimpleMeterRegistry();
		new RabbitMetrics(connectionFactory, null).bindTo(registry);
		registry.get("rabbitmq.connections");
	}

	@Test
	void connectionFactoryWithTagsIsInstrumented() {
		ConnectionFactory connectionFactory = mock(ConnectionFactory.class);
		SimpleMeterRegistry registry = new SimpleMeterRegistry();
		new RabbitMetrics(connectionFactory, Tags.of("env", "prod")).bindTo(registry);
		assertThat(registry.get("rabbitmq.connections").tags("env", "prod").meter()).isNotNull();
		assertThat(registry.find("rabbitmq.connections").tags("env", "dev").meter()).isNull();
	}

}
