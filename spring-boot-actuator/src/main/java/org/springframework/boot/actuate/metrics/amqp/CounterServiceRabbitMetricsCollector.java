/*
 * Copyright 2012-2017 the original author or authors.
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

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.impl.AbstractMetricsCollector;

import org.springframework.boot.actuate.metrics.CounterService;

/**
 * Collects RabbitMQ Client metrics with {@link CounterService}.
 *
 * @author Arnaud Cogoluègnes
 * @since 2.0.0
 */
public class CounterServiceRabbitMetricsCollector extends AbstractMetricsCollector {

	private final CounterService counterService;
	private final String connectionsMetricName;
	private final String channelsMetricName;
	private final String publishedMessagesMetricName;
	private final String consumedMessagesMetricName;
	private final String acknowledgedMessagesMetricName;
	private final String rejectedMessagesMetricName;

	public CounterServiceRabbitMetricsCollector(CounterService counterService) {
		this(counterService, "rabbitmq");
	}

	public CounterServiceRabbitMetricsCollector(CounterService counterService, String metricsPrefix) {
		this.counterService = counterService;
		this.connectionsMetricName = metricsPrefix + ".connections";
		this.channelsMetricName = metricsPrefix + ".channels";
		this.publishedMessagesMetricName = metricsPrefix + ".published";
		this.consumedMessagesMetricName = metricsPrefix + ".consumed";
		this.acknowledgedMessagesMetricName = metricsPrefix + ".acknowledged";
		this.rejectedMessagesMetricName = metricsPrefix + ".rejected";
	}

	@Override
	protected void incrementConnectionCount(Connection connection) {
		this.counterService.increment(this.connectionsMetricName);
	}

	@Override
	protected void decrementConnectionCount(Connection connection) {
		this.counterService.decrement(this.connectionsMetricName);
	}

	@Override
	protected void incrementChannelCount(Channel channel) {
		this.counterService.increment(this.channelsMetricName);
	}

	@Override
	protected void decrementChannelCount(Channel channel) {
		this.counterService.decrement(this.channelsMetricName);
	}

	@Override
	protected void markPublishedMessage() {
		this.counterService.increment(this.publishedMessagesMetricName);
	}

	@Override
	protected void markConsumedMessage() {
		this.counterService.increment(this.consumedMessagesMetricName);
	}

	@Override
	protected void markAcknowledgedMessage() {
		this.counterService.increment(this.acknowledgedMessagesMetricName);
	}

	@Override
	protected void markRejectedMessage() {
		this.counterService.increment(this.rejectedMessagesMetricName);
	}

	public void reset() {
		this.counterService.reset(this.connectionsMetricName);
		this.counterService.reset(this.channelsMetricName);
		this.counterService.reset(this.publishedMessagesMetricName);
		this.counterService.reset(this.consumedMessagesMetricName);
		this.counterService.reset(this.acknowledgedMessagesMetricName);
		this.counterService.reset(this.rejectedMessagesMetricName);
	}
}
