/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.reactor.netty.autoconfigure;

import java.time.Duration;

import org.jspecify.annotations.Nullable;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for Reactor Netty.
 *
 * @author Moritz Halbritter
 * @since 4.0.0
 */
@ConfigurationProperties("spring.reactor.netty")
public class ReactorNettyProperties {

	/**
	 * Amount of time to wait before shutting down resources.
	 */
	private @Nullable Duration shutdownQuietPeriod;

	public @Nullable Duration getShutdownQuietPeriod() {
		return this.shutdownQuietPeriod;
	}

	public void setShutdownQuietPeriod(@Nullable Duration shutdownQuietPeriod) {
		this.shutdownQuietPeriod = shutdownQuietPeriod;
	}

}
