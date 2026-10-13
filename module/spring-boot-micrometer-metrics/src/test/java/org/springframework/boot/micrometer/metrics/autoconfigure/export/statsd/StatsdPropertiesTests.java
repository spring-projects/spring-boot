/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.micrometer.metrics.autoconfigure.export.statsd;

import io.micrometer.statsd.StatsdConfig;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link StatsdProperties}.
 *
 * @author Stephane Nicoll
 */
class StatsdPropertiesTests {

	@Test
	void defaultValuesAreConsistent() {
		StatsdProperties properties = new StatsdProperties();
		StatsdConfig config = StatsdConfig.DEFAULT;
		assertThat(properties.isEnabled()).isEqualTo(config.enabled());
		assertThat(properties.getFlavor()).isEqualTo(config.flavor());
		assertThat(properties.getHost()).isEqualTo(config.host());
		assertThat(properties.getPort()).isEqualTo(config.port());
		assertThat(properties.getProtocol()).isEqualTo(config.protocol());
		assertThat(properties.getMaxPacketLength()).isEqualTo(config.maxPacketLength());
		assertThat(properties.getPollingFrequency()).isEqualTo(config.pollingFrequency());
		assertThat(properties.getStep()).isEqualTo(config.step());
		assertThat(properties.isPublishUnchangedMeters()).isEqualTo(config.publishUnchangedMeters());
		assertThat(properties.isBuffered()).isEqualTo(config.buffered());
	}

}
