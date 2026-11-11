/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.micrometer.metrics.autoconfigure.export.properties;

import io.micrometer.core.instrument.push.PushRegistryConfig;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Base tests for {@link PushRegistryProperties} implementation.
 *
 * @author Stephane Nicoll
 */
public abstract class PushRegistryPropertiesTests {

	@SuppressWarnings("deprecation")
	protected void assertStepRegistryDefaultValues(PushRegistryProperties properties, PushRegistryConfig config) {
		assertThat(properties.getStep()).isEqualTo(config.step());
		assertThat(properties.isEnabled()).isEqualTo(config.enabled());
		assertThat(properties.getConnectTimeout()).isEqualTo(config.connectTimeout());
		assertThat(properties.getReadTimeout()).isEqualTo(config.readTimeout());
		assertThat(properties.getBatchSize()).isEqualTo(config.batchSize());
	}

}
