/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.actuate.autoconfigure.startup;

import org.junit.jupiter.api.Test;

import org.springframework.boot.actuate.startup.StartupEndpoint;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.context.metrics.buffering.BufferingApplicationStartup;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link StartupEndpointAutoConfiguration}
 *
 * @author Brian Clozel
 */
class StartupEndpointAutoConfigurationTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(StartupEndpointAutoConfiguration.class));

	@Test
	void runShouldNotHaveStartupEndpoint() {
		this.contextRunner.run((context) -> assertThat(context).doesNotHaveBean(StartupEndpoint.class));
	}

	@Test
	void runWhenMissingAppStartupShouldNotHaveStartupEndpoint() {
		this.contextRunner.withPropertyValues("management.endpoints.web.exposure.include=startup")
			.run((context) -> assertThat(context).doesNotHaveBean(StartupEndpoint.class));
	}

	@Test
	void runShouldHaveStartupEndpoint() {
		new ApplicationContextRunner(() -> {
			AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
			context.setApplicationStartup(new BufferingApplicationStartup(1));
			return context;
		}).withConfiguration(AutoConfigurations.of(StartupEndpointAutoConfiguration.class))
			.withPropertyValues("management.endpoints.web.exposure.include=startup")
			.run((context) -> assertThat(context).hasSingleBean(StartupEndpoint.class));
	}

}
