/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.netty.autoconfigure;

import io.netty.util.ResourceLeakDetector;
import io.netty.util.ResourceLeakDetector.Level;
import org.junit.jupiter.api.Test;

import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link NettyProperties}
 *
 * @author Brian Clozel
 */
class NettyPropertiesTests {

	@Test
	void defaultValueShouldBeConsistent() {
		ResourceLeakDetector.Level defaultLevel = (Level) ReflectionTestUtils.getField(ResourceLeakDetector.class,
				"DEFAULT_LEVEL");
		assertThat(defaultLevel).isEqualTo(Level.SIMPLE);
	}

}
