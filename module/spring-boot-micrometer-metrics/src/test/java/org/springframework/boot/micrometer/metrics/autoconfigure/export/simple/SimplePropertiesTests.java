/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.micrometer.metrics.autoconfigure.export.simple;

import io.micrometer.core.instrument.simple.SimpleConfig;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Stephane Nicoll
 */
class SimplePropertiesTests {

	@Test
	void defaultValuesAreConsistent() {
		SimpleProperties properties = new SimpleProperties();
		SimpleConfig config = SimpleConfig.DEFAULT;
		assertThat(properties.getStep()).isEqualTo(config.step());
		assertThat(properties.getMode()).isEqualTo(config.mode());
	}

}
