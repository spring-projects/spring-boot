/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.micrometer.observation.autoconfigure;

import java.util.ArrayList;
import java.util.List;

import io.micrometer.observation.ObservationRegistry;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link ObservationRegistryConfigurer} and
 * {@link ObservationRegistryPostProcessor}.
 *
 * @author Moritz Halbritter
 */
class ObservationRegistryConfigurerIntegrationTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(ObservationAutoConfiguration.class));

	@Test
	void customizersAreCalledInOrder() {
		this.contextRunner.withUserConfiguration(Customizers.class).run((context) -> {
			CalledCustomizers calledCustomizers = context.getBean(CalledCustomizers.class);
			Customizer1 customizer1 = context.getBean(Customizer1.class);
			Customizer2 customizer2 = context.getBean(Customizer2.class);
			assertThat(calledCustomizers.getCustomizers()).containsExactly(customizer1, customizer2);
		});
	}

	@Configuration(proxyBeanMethods = false)
	static class Customizers {

		@Bean
		CalledCustomizers calledCustomizers() {
			return new CalledCustomizers();
		}

		@Bean
		@Order(1)
		Customizer1 customizer1(CalledCustomizers calledCustomizers) {
			return new Customizer1(calledCustomizers);
		}

		@Bean
		@Order(2)
		Customizer2 customizer2(CalledCustomizers calledCustomizers) {
			return new Customizer2(calledCustomizers);
		}

	}

	private static final class CalledCustomizers {

		private final List<ObservationRegistryCustomizer<?>> customizers = new ArrayList<>();

		void onCalled(ObservationRegistryCustomizer<?> customizer) {
			this.customizers.add(customizer);
		}

		List<ObservationRegistryCustomizer<?>> getCustomizers() {
			return this.customizers;
		}

	}

	private static class Customizer1 implements ObservationRegistryCustomizer<ObservationRegistry> {

		private final CalledCustomizers calledCustomizers;

		Customizer1(CalledCustomizers calledCustomizers) {
			this.calledCustomizers = calledCustomizers;
		}

		@Override
		public void customize(ObservationRegistry registry) {
			this.calledCustomizers.onCalled(this);
		}

	}

	private static class Customizer2 implements ObservationRegistryCustomizer<ObservationRegistry> {

		private final CalledCustomizers calledCustomizers;

		Customizer2(CalledCustomizers calledCustomizers) {
			this.calledCustomizers = calledCustomizers;
		}

		@Override
		public void customize(ObservationRegistry registry) {
			this.calledCustomizers.onCalled(this);
		}

	}

}
