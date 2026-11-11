/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.micrometer.tracing.test.autoconfigure;

import io.micrometer.tracing.Tracer;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test to verify behaviour when
 * {@link AutoConfigureTracing @AutoConfigureTracing} is present on the test class.
 *
 * @author Chris Bono
 * @author Moritz Halbritter
 */
@SpringBootTest
@AutoConfigureTracing
class AutoConfigureTracingPresentIntegrationTests {

	@Test
	void customizerDoesNotDisableAvailableMeterRegistriesWhenAnnotationPresent(
			@Autowired ApplicationContext applicationContext) {
		assertThat(applicationContext.getBean(Tracer.class)).isEqualTo(Tracer.NOOP);
	}

	@Test
	void customizerDoesNotSetExportDisabledPropertyWhenAnnotationPresent(@Autowired Environment environment) {
		assertThat(environment.containsProperty("management.tracing.export.enabled")).isFalse();
	}

}
