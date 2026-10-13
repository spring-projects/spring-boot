/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.docs.features.externalconfig.typesafeconfigurationproperties.conversion.durations.javabeanbinding;

import java.util.function.Consumer;

import org.junit.jupiter.api.Test;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.assertj.AssertableApplicationContext;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.context.runner.ContextConsumer;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link MyProperties}.
 *
 * @author Stephane Nicoll
 */
class MyPropertiesTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withUserConfiguration(Config.class);

	@Test
	void bindWithDefaultUnit() {
		this.contextRunner.withPropertyValues("my.session-timeout=40", "my.read-timeout=5000")
			.run(assertBinding((properties) -> {
				assertThat(properties.getSessionTimeout()).hasSeconds(40);
				assertThat(properties.getReadTimeout()).hasMillis(5000);
			}));
	}

	@Test
	void bindWithExplicitUnit() {
		this.contextRunner.withPropertyValues("my.session-timeout=1h", "my.read-timeout=5s")
			.run(assertBinding((properties) -> {
				assertThat(properties.getSessionTimeout()).hasMinutes(60);
				assertThat(properties.getReadTimeout()).hasMillis(5000);
			}));
	}

	@Test
	void bindWithIso8601Format() {
		this.contextRunner.withPropertyValues("my.session-timeout=PT15S", "my.read-timeout=PT0.5S")
			.run(assertBinding((properties) -> {
				assertThat(properties.getSessionTimeout()).hasSeconds(15);
				assertThat(properties.getReadTimeout()).hasMillis(500);
			}));
	}

	private ContextConsumer<AssertableApplicationContext> assertBinding(Consumer<MyProperties> properties) {
		return (context) -> {
			assertThat(context).hasSingleBean(MyProperties.class);
			properties.accept(context.getBean(MyProperties.class));
		};
	}

	@Configuration(proxyBeanMethods = false)
	@EnableConfigurationProperties(MyProperties.class)
	static class Config {

	}

}
