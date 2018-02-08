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

package org.springframework.boot.actuate.autoconfigure.kafka;

import java.time.Duration;

import org.springframework.boot.actuate.kafka.KafkaHealthIndicator;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for {@link KafkaHealthIndicator}.
 *
 * @author Juan Rada
 * @since 2.0.0
 */
@ConfigurationProperties(prefix = "management.health.kafka", ignoreUnknownFields = false)
public class KafkaHealthIndicatorProperties {

	/**
	 * Time to wait for a response from the cluster description operation.
	 */
	private Duration responseTimeout = Duration.ofMillis(1000);

	public Duration getResponseTimeout() {
		return this.responseTimeout;
	}

	public void setResponseTimeout(Duration responseTimeout) {
		this.responseTimeout = responseTimeout;
	}

}
