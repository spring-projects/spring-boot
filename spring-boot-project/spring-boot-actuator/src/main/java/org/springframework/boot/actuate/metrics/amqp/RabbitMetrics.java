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
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.impl.AbstractMetricsCollector;
import io.micrometer.core.instrument.Counter;
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

	/**
	 * Micrometer implementation of {@link com.rabbitmq.client.MetricsCollector}.
	 */
	public static class MicrometerMetricsCollector extends AbstractMetricsCollector {

		private final AtomicLong connections;

		private final AtomicLong channels;

		private final Counter publishedMessages;

		private final Counter consumedMessages;

		private final Counter acknowledgedMessages;

		private final Counter rejectedMessages;

		public MicrometerMetricsCollector(MeterRegistry registry) {
			this(registry, "rabbitmq", Collections.emptyList());
		}

		public MicrometerMetricsCollector(final MeterRegistry registry, final String prefix, Iterable<Tag> tags) {
			this(metric -> metric.create(registry, prefix, tags));
		}

		public MicrometerMetricsCollector(Function<Metrics, Object> metricsCreator) {
			this.connections = (AtomicLong) metricsCreator.apply(
					org.springframework.boot.actuate.metrics.amqp.RabbitMetrics.MicrometerMetricsCollector.Metrics.CONNECTIONS);
			this.channels = (AtomicLong) metricsCreator.apply(
					org.springframework.boot.actuate.metrics.amqp.RabbitMetrics.MicrometerMetricsCollector.Metrics.CHANNELS);
			this.publishedMessages = (Counter) metricsCreator.apply(
					org.springframework.boot.actuate.metrics.amqp.RabbitMetrics.MicrometerMetricsCollector.Metrics.PUBLISHED_MESSAGES);
			this.consumedMessages = (Counter) metricsCreator.apply(
					org.springframework.boot.actuate.metrics.amqp.RabbitMetrics.MicrometerMetricsCollector.Metrics.CONSUMED_MESSAGES);
			this.acknowledgedMessages = (Counter) metricsCreator.apply(
					org.springframework.boot.actuate.metrics.amqp.RabbitMetrics.MicrometerMetricsCollector.Metrics.ACKNOWLEDGED_MESSAGES);
			this.rejectedMessages = (Counter) metricsCreator.apply(
					org.springframework.boot.actuate.metrics.amqp.RabbitMetrics.MicrometerMetricsCollector.Metrics.REJECTED_MESSAGES);
		}

		@Override
		protected void incrementConnectionCount(Connection connection) {
			this.connections.incrementAndGet();
		}

		@Override
		protected void decrementConnectionCount(Connection connection) {
			this.connections.decrementAndGet();
		}

		@Override
		protected void incrementChannelCount(Channel channel) {
			this.channels.incrementAndGet();
		}

		@Override
		protected void decrementChannelCount(Channel channel) {
			this.channels.decrementAndGet();
		}

		@Override
		protected void markPublishedMessage() {
			this.publishedMessages.increment();
		}

		@Override
		protected void markConsumedMessage() {
			this.consumedMessages.increment();
		}

		@Override
		protected void markAcknowledgedMessage() {
			this.acknowledgedMessages.increment();
		}

		@Override
		protected void markRejectedMessage() {
			this.rejectedMessages.increment();
		}

		/**
		 * RabbitMQ client metrics types.
		 */
		public enum Metrics {
			/**
			 * Number of connections metrics.
			 */
			CONNECTIONS {
				@Override
				Object create(MeterRegistry registry, String prefix, Iterable<Tag> tags) {
					return registry.gauge(prefix + ".connections", tags, new AtomicLong(0));
				}
			},
			/**
			 * Number of channels metrics.
			 */
			CHANNELS {
				@Override
				Object create(MeterRegistry registry, String prefix, Iterable<Tag> tags) {
					return registry.gauge(prefix + ".channels", tags, new AtomicLong(0));
				}
			},
			/**
			 * Published messages metrics.
			 */
			PUBLISHED_MESSAGES {
				@Override
				Object create(MeterRegistry registry, String prefix, Iterable<Tag> tags) {
					return registry.counter(prefix + "published", tags);
				}
			},
			/**
			 * Consumed messages metrics.
			 */
			CONSUMED_MESSAGES {
				@Override
				Object create(MeterRegistry registry, String prefix, Iterable<Tag> tags) {
					return registry.counter(prefix + ".consumed", tags);
				}
			},
			/**
			 * Acknowledged messages metrics.
			 */
			ACKNOWLEDGED_MESSAGES {
				@Override
				Object create(MeterRegistry registry, String prefix, Iterable<Tag> tags) {
					return registry.counter(prefix + ".acknowledged", tags);
				}
			},
			/**
			 * Rejected messages metrics.
			 */
			REJECTED_MESSAGES {
				@Override
				Object create(MeterRegistry registry, String prefix, Iterable<Tag> tags) {
					return registry.counter(prefix + ".rejected", tags);
				}
			};

			abstract Object create(MeterRegistry registry, String prefix, Iterable<Tag> tags);
		}

	}

}
