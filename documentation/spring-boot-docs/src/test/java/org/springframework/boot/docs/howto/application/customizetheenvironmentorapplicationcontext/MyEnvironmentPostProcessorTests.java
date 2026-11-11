/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.docs.howto.application.customizetheenvironmentorapplicationcontext;

import org.junit.jupiter.api.Test;

import org.springframework.boot.SpringApplication;
import org.springframework.core.env.StandardEnvironment;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link MyEnvironmentPostProcessor}.
 *
 * @author Stephane Nicoll
 */
class MyEnvironmentPostProcessorTests {

	private final StandardEnvironment environment = new StandardEnvironment();

	@Test
	void applyEnvironmentPostProcessor() {
		assertThat(this.environment.containsProperty("test.foo.bar")).isFalse();
		new MyEnvironmentPostProcessor().postProcessEnvironment(this.environment, new SpringApplication());
		assertThat(this.environment.containsProperty("test.foo.bar")).isTrue();
		assertThat(this.environment.getProperty("test.foo.bar")).isEqualTo("value");
	}

}
