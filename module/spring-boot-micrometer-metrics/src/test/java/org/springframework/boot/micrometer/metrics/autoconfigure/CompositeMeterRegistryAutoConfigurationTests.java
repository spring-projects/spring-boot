/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.micrometer.metrics.autoconfigure;

import io.micrometer.core.instrument.Clock;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.composite.CompositeMeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link CompositeMeterRegistryAutoConfiguration}.
 *
 * @author Phillip Webb
 * @author Andy Wilkinson
 */
class CompositeMeterRegistryAutoConfigurationTests {

	private static final String COMPOSITE_NAME = "compositeMeterRegistry";

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withUserConfiguration(BaseConfig.class)
		.withConfiguration(AutoConfigurations.of(CompositeMeterRegistryAutoConfiguration.class));

	@Test
	void registerWhenHasNoMeterRegistryShouldRegisterEmptyNoOpComposite() {
		this.contextRunner.withUserConfiguration(NoMeterRegistryConfig.class).run((context) -> {
			assertThat(context).hasSingleBean(MeterRegistry.class);
			CompositeMeterRegistry registry = context.getBean("noOpMeterRegistry", CompositeMeterRegistry.class);
			assertThat(registry.getRegistries()).isEmpty();
		});
	}

	@Test
	void registerWhenHasSingleMeterRegistryShouldDoNothing() {
		this.contextRunner.withUserConfiguration(SingleMeterRegistryConfig.class).run((context) -> {
			assertThat(context).hasSingleBean(MeterRegistry.class);
			MeterRegistry registry = context.getBean(MeterRegistry.class);
			assertThat(registry).isInstanceOf(TestMeterRegistry.class);
		});
	}

	@Test
	void registerWhenHasMultipleMeterRegistriesShouldAddPrimaryComposite() {
		this.contextRunner.withUserConfiguration(MultipleMeterRegistriesConfig.class).run((context) -> {
			assertThat(context.getBeansOfType(MeterRegistry.class)).hasSize(3)
				.containsKeys("meterRegistryOne", "meterRegistryTwo", COMPOSITE_NAME);
			MeterRegistry primary = context.getBean(MeterRegistry.class);
			assertThat(primary).isInstanceOf(CompositeMeterRegistry.class);
			assertThat(((CompositeMeterRegistry) primary).getRegistries()).hasSize(2);
			assertThat(primary.config().clock()).isNotNull();
		});
	}

	@Test
	void registerWhenHasMultipleRegistriesAndOneIsPrimaryShouldDoNothing() {
		this.contextRunner.withUserConfiguration(MultipleMeterRegistriesWithOnePrimaryConfig.class).run((context) -> {
			assertThat(context.getBeansOfType(MeterRegistry.class)).hasSize(2)
				.containsKeys("meterRegistryOne", "meterRegistryTwo");
			MeterRegistry primary = context.getBean(MeterRegistry.class);
			assertThat(primary).isInstanceOf(TestMeterRegistry.class);
		});
	}

	@Configuration(proxyBeanMethods = false)
	static class BaseConfig {

		@Bean
		@ConditionalOnMissingBean
		Clock micrometerClock() {
			return Clock.SYSTEM;
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class NoMeterRegistryConfig {

	}

	@Configuration(proxyBeanMethods = false)
	static class SingleMeterRegistryConfig {

		@Bean
		MeterRegistry meterRegistry() {
			return new TestMeterRegistry();
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class MultipleMeterRegistriesConfig {

		@Bean
		MeterRegistry meterRegistryOne() {
			return new TestMeterRegistry();
		}

		@Bean
		MeterRegistry meterRegistryTwo() {
			return new SimpleMeterRegistry();
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class MultipleMeterRegistriesWithOnePrimaryConfig {

		@Bean
		@Primary
		MeterRegistry meterRegistryOne() {
			return new TestMeterRegistry();
		}

		@Bean
		MeterRegistry meterRegistryTwo() {
			return new SimpleMeterRegistry();
		}

	}

	static class TestMeterRegistry extends SimpleMeterRegistry {

	}

}
