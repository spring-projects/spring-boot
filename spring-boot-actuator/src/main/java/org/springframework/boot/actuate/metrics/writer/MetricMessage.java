/*
 * Copyright 2012-2015 the original author or authors.
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

package org.springframework.boot.actuate.metrics.writer;

import org.springframework.boot.actuate.metrics.Metric;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;

/**
 * A metric message sent via Spring Integration.
 *
 * @author Phillip Webb
 */
class MetricMessage {

	private static final String METRIC_NAME = "metricName";

	private static final String DELETE = "delete";

	private final Message<?> message;

	MetricMessage(Message<?> message) {
		this.message = message;
	}

	public boolean isReset() {
		return DELETE.equals(getPayload());
	}

	public Object getPayload() {
		return this.message.getPayload();
	}

	public String getMetricName() {
		return this.message.getHeaders().get(METRIC_NAME, String.class);
	}

	public static Message<?> forIncrement(Delta<?> delta) {
		return forPayload(delta.getName(), delta);
	}

	public static Message<?> forSet(Metric<?> value) {
		return forPayload(value.getName(), value);
	}

	public static Message<?> forReset(String metricName) {
		return forPayload(metricName, DELETE);
	}

	private static Message<?> forPayload(String metricName, Object payload) {
		MessageBuilder<Object> builder = MessageBuilder.withPayload(payload);
		builder.setHeader(METRIC_NAME, metricName);
		return builder.build();
	}

}
