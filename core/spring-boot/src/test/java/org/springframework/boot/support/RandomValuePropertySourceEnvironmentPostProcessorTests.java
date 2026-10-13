/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.support;

import java.util.function.Supplier;

import org.junit.jupiter.api.Test;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.context.config.ConfigDataEnvironmentPostProcessor;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link RandomValuePropertySourceEnvironmentPostProcessor}.
 *
 * @author Phillip Webb
 */
class RandomValuePropertySourceEnvironmentPostProcessorTests {

	private final RandomValuePropertySourceEnvironmentPostProcessor postProcessor = new RandomValuePropertySourceEnvironmentPostProcessor(
			Supplier::get);

	@Test
	void getOrderIsBeforeConfigData() {
		assertThat(this.postProcessor.getOrder()).isLessThan(ConfigDataEnvironmentPostProcessor.ORDER);
	}

	@Test
	void postProcessEnvironmentAddsPropertySource() {
		MockEnvironment environment = new MockEnvironment();
		this.postProcessor.postProcessEnvironment(environment, mock(SpringApplication.class));
		assertThat(environment.getProperty("random.string")).isNotNull();
	}

}
