/*
 * Copyright 2012-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.actuate.metrics.amqp;

import com.rabbitmq.client.ConnectionFactory;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link RabbitMetrics}.
 *
 * @author Stephane Nicoll
 */
public class RabbitMetricsTests {

	@Test
	public void connectionFactoryIsInstrumented() {
		ConnectionFactory connectionFactory = mock(ConnectionFactory.class);
		SimpleMeterRegistry registry = new SimpleMeterRegistry();
		new RabbitMetrics(connectionFactory, null).bindTo(registry);
		registry.get("rabbitmq.connections");
	}

	@Test
	public void connectionFactoryWithTagsIsInstrumented() {
		ConnectionFactory connectionFactory = mock(ConnectionFactory.class);
		SimpleMeterRegistry registry = new SimpleMeterRegistry();
		new RabbitMetrics(connectionFactory, Tags.of("env", "prod")).bindTo(registry);
		assertThat(registry.get("rabbitmq.connections").tags("env", "prod").meter())
				.isNotNull();
		assertThat(registry.find("rabbitmq.connections").tags("env", "dev").meter())
				.isNull();
	}

}
