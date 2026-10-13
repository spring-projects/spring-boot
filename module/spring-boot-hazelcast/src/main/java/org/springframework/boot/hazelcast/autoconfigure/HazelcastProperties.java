/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.hazelcast.autoconfigure;

import org.jspecify.annotations.Nullable;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.io.Resource;
import org.springframework.util.Assert;

/**
 * Configuration properties for the hazelcast integration.
 *
 * @author Stephane Nicoll
 * @since 4.0.0
 */
@ConfigurationProperties("spring.hazelcast")
public class HazelcastProperties {

	/**
	 * The location of the configuration file to use to initialize Hazelcast.
	 */
	private @Nullable Resource config;

	public @Nullable Resource getConfig() {
		return this.config;
	}

	public void setConfig(@Nullable Resource config) {
		this.config = config;
	}

	/**
	 * Resolve the config location if set.
	 * @return the location or {@code null} if it is not set
	 */
	public @Nullable Resource resolveConfigLocation() {
		Resource config = this.config;
		if (config == null) {
			return null;
		}
		Assert.state(config.exists(), () -> "Hazelcast configuration does not exist '" + config.getDescription() + "'");
		return config;
	}

}
