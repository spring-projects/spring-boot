/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.autoconfigure.availability;

import org.junit.jupiter.api.Test;

import org.springframework.boot.LazyInitializationBeanFactoryPostProcessor;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.availability.ApplicationAvailability;
import org.springframework.boot.availability.AvailabilityChangeEvent;
import org.springframework.boot.availability.ReadinessState;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link ApplicationAvailabilityAutoConfiguration}
 *
 * @author Brian Clozel
 * @author Taeik Lim
 */
class ApplicationAvailabilityAutoConfigurationTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(ApplicationAvailabilityAutoConfiguration.class));

	@Test
	void providerIsPresentWhenNotRegistered() {
		this.contextRunner.run(((context) -> assertThat(context).hasSingleBean(ApplicationAvailability.class)
			.hasBean("applicationAvailability")));
	}

	@Test
	void providerIsNotConfiguredWhenCustomOneIsPresent() {
		this.contextRunner
			.withBean("customApplicationAvailability", ApplicationAvailability.class,
					() -> mock(ApplicationAvailability.class))
			.run(((context) -> assertThat(context).hasSingleBean(ApplicationAvailability.class)
				.hasBean("customApplicationAvailability")));
	}

	@Test
	void whenLazyInitializationIsEnabledApplicationAvailabilityBeanShouldStillReceiveAvailabilityChangeEvents() {
		this.contextRunner.withBean(LazyInitializationBeanFactoryPostProcessor.class).run((context) -> {
			AvailabilityChangeEvent.publish(context, ReadinessState.ACCEPTING_TRAFFIC);
			ApplicationAvailability applicationAvailability = context.getBean(ApplicationAvailability.class);
			assertThat(applicationAvailability.getLastChangeEvent(ReadinessState.class)).isNotNull();
		});
	}

}
