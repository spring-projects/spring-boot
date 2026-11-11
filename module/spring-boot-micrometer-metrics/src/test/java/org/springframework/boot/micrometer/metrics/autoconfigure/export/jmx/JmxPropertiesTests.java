/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.micrometer.metrics.autoconfigure.export.jmx;

import io.micrometer.jmx.JmxConfig;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link JmxProperties}.
 *
 * @author Stephane Nicoll
 */
class JmxPropertiesTests {

	@Test
	void defaultValuesAreConsistent() {
		JmxProperties properties = new JmxProperties();
		JmxConfig config = JmxConfig.DEFAULT;
		assertThat(properties.getDomain()).isEqualTo(config.domain());
		assertThat(properties.getStep()).isEqualTo(config.step());
	}

}
