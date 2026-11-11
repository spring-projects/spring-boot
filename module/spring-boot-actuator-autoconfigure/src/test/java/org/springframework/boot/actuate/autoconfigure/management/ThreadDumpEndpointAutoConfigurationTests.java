/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.actuate.autoconfigure.management;

import org.junit.jupiter.api.Test;

import org.springframework.boot.actuate.management.ThreadDumpEndpoint;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ThreadDumpEndpointAutoConfiguration}.
 *
 * @author Phillip Webb
 * @author Moritz Halbritter
 */
class ThreadDumpEndpointAutoConfigurationTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(ThreadDumpEndpointAutoConfiguration.class));

	@Test
	void runShouldHaveEndpointBean() {
		this.contextRunner.withPropertyValues("management.endpoints.web.exposure.include=threaddump")
			.run((context) -> assertThat(context).hasSingleBean(ThreadDumpEndpoint.class));
	}

	@Test
	void runWhenNotExposedShouldNotHaveEndpointBean() {
		this.contextRunner.run((context) -> assertThat(context).doesNotHaveBean(ThreadDumpEndpoint.class));
	}

	@Test
	void runWhenEnabledPropertyIsFalseShouldNotHaveEndpointBean() {
		this.contextRunner.withPropertyValues("management.endpoints.web.exposure.include=*")
			.withPropertyValues("management.endpoint.threaddump.enabled:false")
			.run((context) -> assertThat(context).doesNotHaveBean(ThreadDumpEndpoint.class));
	}

}
