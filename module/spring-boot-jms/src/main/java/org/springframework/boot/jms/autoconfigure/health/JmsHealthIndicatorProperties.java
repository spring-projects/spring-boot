/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.jms.autoconfigure.health;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jms.health.JmsHealthIndicator;

/**
 * Configuration properties for {@link JmsHealthIndicator}.
 *
 * @author Venkata Naga Sai Srikanth Gollapudi
 * @author Stephane Nicoll
 * @since 4.2.0
 */
@ConfigurationProperties("management.health.jms")
public class JmsHealthIndicatorProperties {

	/**
	 * Timeout to use when starting a connection for the health check.
	 */
	private Duration startTimeout = JmsHealthIndicator.DEFAULT_START_TIMEOUT;

	public Duration getStartTimeout() {
		return this.startTimeout;
	}

	public void setStartTimeout(Duration startTimeout) {
		this.startTimeout = startTimeout;
	}

}
