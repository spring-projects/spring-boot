/*
 * Copyright 2012-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.actuate.metrics.amqp;

import java.util.Collections;

import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.impl.MicrometerMetricsCollector;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.binder.MeterBinder;

/**
 * A {@link MeterBinder} for RabbitMQ Java Client metrics.
 *
 * @author Arnaud Cogoluègnes
 * @since 2.0.0
 */
public class RabbitMetrics implements MeterBinder {

	private final Iterable<Tag> tags;

	private final ConnectionFactory connectionFactory;

	public RabbitMetrics(ConnectionFactory connectionFactory) {
		this(connectionFactory, Collections.emptyList());
	}

	public RabbitMetrics(ConnectionFactory connectionFactory, Iterable<Tag> tags) {
		this.connectionFactory = connectionFactory;
		this.tags = tags;
	}

	@Override
	public void bindTo(MeterRegistry registry) {
		this.connectionFactory.setMetricsCollector(new MicrometerMetricsCollector(registry, "rabbitmq", this.tags));
	}

}
