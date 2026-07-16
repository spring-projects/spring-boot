/*
 * Copyright 2012-present the original author or authors.
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

package org.springframework.boot.jms.autoconfigure.health;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jms.health.JmsHealthIndicator;
import org.springframework.util.Assert;

/**
 * External configuration properties for {@link JmsHealthIndicator}.
 *
 * @author Venkata Naga Sai Srikanth Gollapudi
 * @since 4.x
 */
@ConfigurationProperties("management.health.jms")
public class JmsHealthIndicatorProperties {

	/**
	 * Timeout to use when starting a connection for the health check.
	 */
	private Duration timeout = Duration.ofSeconds(5);

	public Duration getTimeout() {
		return this.timeout;
	}

	public void setTimeout(Duration timeout) {
		Assert.notNull(timeout, "'timeout' must not be null");
		Assert.isTrue(timeout.compareTo(Duration.ZERO) > 0, "'timeout' must be greater than 0");
		this.timeout = timeout;
	}

}
