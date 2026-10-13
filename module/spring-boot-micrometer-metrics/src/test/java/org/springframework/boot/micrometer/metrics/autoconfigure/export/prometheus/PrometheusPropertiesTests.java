/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.micrometer.metrics.autoconfigure.export.prometheus;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link PrometheusProperties}.
 *
 * @author Stephane Nicoll
 */
class PrometheusPropertiesTests {

	@Test
	void defaultValuesAreConsistent() {
		PrometheusProperties properties = new PrometheusProperties();
		io.micrometer.prometheusmetrics.PrometheusConfig config = io.micrometer.prometheusmetrics.PrometheusConfig.DEFAULT;
		assertThat(properties.isDescriptions()).isEqualTo(config.descriptions());
		assertThat(properties.getStep()).isEqualTo(config.step());
	}

}
