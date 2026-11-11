/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.micrometer.tracing.test.autoconfigure;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test to verify behaviour when
 * {@link AutoConfigureTracing @AutoConfigureTracing} is not present on the test class.
 *
 * @author Chris Bono
 * @author Moritz Halbritter
 */
@SpringBootTest
class AutoConfigureTracingMissingIntegrationTests {

	@Test
	void customizerRunsAndDisablesExportWhenNoAnnotationPresent(@Autowired Environment environment) {
		assertThat(environment.getProperty("management.tracing.export.enabled")).isEqualTo("false");
	}

}
