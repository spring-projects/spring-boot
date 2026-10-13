/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.autoconfigure.condition;

import org.junit.jupiter.api.Test;

import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.testsupport.classpath.ClassPathOverrides;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ConditionalOnCheckpointRestore @ConditionalOnCheckpointRestore}.
 *
 * @author Andy Wilkinson
 */
class ConditionalOnCheckpointRestoreTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withUserConfiguration(BasicConfiguration.class);

	@Test
	void whenCracIsUnavailableThenConditionDoesNotMatch() {
		this.contextRunner.run((context) -> assertThat(context).doesNotHaveBean("someBean"));
	}

	@Test
	@ClassPathOverrides("org.crac:crac:1.3.0")
	void whenCracIsAvailableThenConditionMatches() {
		this.contextRunner.run((context) -> assertThat(context).hasBean("someBean"));
	}

	@Configuration(proxyBeanMethods = false)
	static class BasicConfiguration {

		@Bean
		@ConditionalOnCheckpointRestore
		String someBean() {
			return "someBean";
		}

	}

}
