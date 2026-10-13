/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.autoconfigure.context;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for lifecycle processing.
 *
 * @author Andy Wilkinson
 * @since 2.3.0
 */
@ConfigurationProperties("spring.lifecycle")
public class LifecycleProperties {

	/**
	 * Timeout for the shutdown of any phase (group of SmartLifecycle beans with the same
	 * 'phase' value).
	 */
	private Duration timeoutPerShutdownPhase = Duration.ofSeconds(30);

	public Duration getTimeoutPerShutdownPhase() {
		return this.timeoutPerShutdownPhase;
	}

	public void setTimeoutPerShutdownPhase(Duration timeoutPerShutdownPhase) {
		this.timeoutPerShutdownPhase = timeoutPerShutdownPhase;
	}

}
