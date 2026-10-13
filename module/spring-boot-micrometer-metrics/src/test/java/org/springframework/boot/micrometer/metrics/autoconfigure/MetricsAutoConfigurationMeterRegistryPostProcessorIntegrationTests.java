/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.micrometer.metrics.autoconfigure;

import java.util.Map;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.MeterBinder;
import io.micrometer.core.instrument.composite.CompositeMeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.micrometer.metrics.autoconfigure.jvm.JvmMetricsAutoConfiguration;
import org.springframework.boot.micrometer.metrics.autoconfigure.logging.logback.LogbackMetricsAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link MeterRegistryPostProcessor} configured by
 * {@link MetricsAutoConfiguration}.
 *
 * @author Jon Schneider
 */
class MetricsAutoConfigurationMeterRegistryPostProcessorIntegrationTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withConfiguration(
				AutoConfigurations.of(MetricsAutoConfiguration.class, CompositeMeterRegistryAutoConfiguration.class))
		.withPropertyValues("management.metrics.use-global-registry=false");

	@Test
	void binderMetricsAreSearchableFromTheComposite() {
		this.contextRunner.withConfiguration(AutoConfigurations.of(JvmMetricsAutoConfiguration.class))
			.run((context) -> {
				CompositeMeterRegistry composite = context.getBean(CompositeMeterRegistry.class);
				composite.get("jvm.memory.used").gauge();
				context.getBeansOfType(MeterRegistry.class)
					.forEach((name, registry) -> registry.get("jvm.memory.used").gauge());
			});
	}

	@Test
	void customizersAreAppliedBeforeBindersAreCreated() {
		this.contextRunner.withUserConfiguration(TestConfiguration.class).run((context) -> {
		});
	}

	@Test
	void counterIsIncrementedOncePerEventWithoutCompositeMeterRegistry() {
		this.contextRunner.withBean(SimpleMeterRegistry.class)
			.withConfiguration(AutoConfigurations.of(LogbackMetricsAutoConfiguration.class))
			.run((context) -> {
				Logger logger = ((LoggerContext) LoggerFactory.getILoggerFactory()).getLogger("test-logger");
				logger.error("Error.");
				Map<String, MeterRegistry> registriesByName = context.getBeansOfType(MeterRegistry.class);
				assertThat(registriesByName).hasSize(1);
				MeterRegistry registry = registriesByName.values().iterator().next();
				assertThat(registry.get("logback.events").tag("level", "error").functionCounter().count()).isOne();
			});
	}

	@Test
	void counterIsIncrementedOncePerEventWithCompositeMeterRegistry() {
		this.contextRunner.withBean("meterRegistry1", SimpleMeterRegistry.class)
			.withBean("meterRegistry2", SimpleMeterRegistry.class)
			.withConfiguration(AutoConfigurations.of(LogbackMetricsAutoConfiguration.class))
			.run((context) -> {
				Logger logger = ((LoggerContext) LoggerFactory.getILoggerFactory()).getLogger("test-logger");
				logger.error("Error.");
				Map<String, MeterRegistry> registriesByName = context.getBeansOfType(MeterRegistry.class);
				assertThat(registriesByName).hasSize(3);
				registriesByName.forEach((name,
						registry) -> assertThat(
								registry.get("logback.events").tag("level", "error").functionCounter().count())
							.isOne());
			});
	}

	@Configuration(proxyBeanMethods = false)
	static class TestConfiguration {

		@Bean
		MeterBinder testBinder(Alpha thing) {
			return (registry) -> {
			};
		}

		@Bean
		MeterRegistryCustomizer<?> testCustomizer() {
			return (registry) -> registry.config().commonTags("testTag", "testValue");
		}

		@Bean
		Alpha alpha() {
			return new Alpha();
		}

		@Bean
		Bravo bravo(Alpha alpha) {
			return new Bravo(alpha);
		}

		@Bean
		static BeanPostProcessor testPostProcessor(ApplicationContext context) {
			return new BeanPostProcessor() {

				@Override
				public Object postProcessAfterInitialization(Object bean, String beanName) {
					if (bean instanceof Bravo) {
						MeterRegistry meterRegistry = context.getBean(MeterRegistry.class);
						meterRegistry.gauge("test", 1);
					}
					return bean;
				}

			};
		}

	}

	static class Alpha {

	}

	static class Bravo {

		Bravo(Alpha alpha) {

		}

	}

}
