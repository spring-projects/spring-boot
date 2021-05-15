/*
 * Copyright 2012-2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.actuate.autoconfigure.metrics;

import java.util.Map;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.MeterBinder;
import io.micrometer.core.instrument.composite.CompositeMeterRegistry;
import org.junit.jupiter.api.Test;
import org.slf4j.impl.StaticLoggerBinder;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.actuate.autoconfigure.metrics.export.atlas.AtlasMetricsExportAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.metrics.export.jmx.JmxMetricsExportAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.metrics.export.prometheus.PrometheusMetricsExportAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.metrics.export.simple.SimpleMetricsExportAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.metrics.test.MetricsRun;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link MeterRegistryConfigurer}.
 *
 * @author Jon Schneider
 */
class MeterRegistryConfigurerIntegrationTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.with(MetricsRun.limitedTo(AtlasMetricsExportAutoConfiguration.class,
					PrometheusMetricsExportAutoConfiguration.class))
			.withConfiguration(AutoConfigurations.of(JvmMetricsAutoConfiguration.class));

	@Test
	void binderMetricsAreSearchableFromTheComposite() {
		this.contextRunner.run((context) -> {
			CompositeMeterRegistry composite = context.getBean(CompositeMeterRegistry.class);
			composite.get("jvm.memory.used").gauge();
			context.getBeansOfType(MeterRegistry.class)
					.forEach((name, registry) -> registry.get("jvm.memory.used").gauge());
		});
	}

	@Test
	void customizersAreAppliedBeforeBindersAreCreated() {
		new ApplicationContextRunner()
				.withConfiguration(AutoConfigurations.of(MetricsAutoConfiguration.class,
						SimpleMetricsExportAutoConfiguration.class))
				.withUserConfiguration(TestConfiguration.class).run((context) -> {

				});
	}

	@Test
	void counterIsIncrementedOncePerEventWithoutCompositeMeterRegistry() {
		new ApplicationContextRunner().with(MetricsRun.limitedTo(JmxMetricsExportAutoConfiguration.class))
				.withConfiguration(AutoConfigurations.of(LogbackMetricsAutoConfiguration.class)).run((context) -> {
					Logger logger = ((LoggerContext) StaticLoggerBinder.getSingleton().getLoggerFactory())
							.getLogger("test-logger");
					logger.error("Error.");
					Map<String, MeterRegistry> registriesByName = context.getBeansOfType(MeterRegistry.class);
					assertThat(registriesByName).hasSize(1);
					MeterRegistry registry = registriesByName.values().iterator().next();
					assertThat(registry.get("logback.events").tag("level", "error").counter().count()).isEqualTo(1);
				});
	}

	@Test
	void counterIsIncrementedOncePerEventWithCompositeMeterRegistry() {
		new ApplicationContextRunner()
				.with(MetricsRun.limitedTo(JmxMetricsExportAutoConfiguration.class,
						PrometheusMetricsExportAutoConfiguration.class))
				.withConfiguration(AutoConfigurations.of(LogbackMetricsAutoConfiguration.class)).run((context) -> {
					Logger logger = ((LoggerContext) StaticLoggerBinder.getSingleton().getLoggerFactory())
							.getLogger("test-logger");
					logger.error("Error.");
					Map<String, MeterRegistry> registriesByName = context.getBeansOfType(MeterRegistry.class);
					assertThat(registriesByName).hasSize(3);
					registriesByName.forEach((name,
							registry) -> assertThat(
									registry.get("logback.events").tag("level", "error").counter().count())
											.isEqualTo(1));
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
				public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
					if (bean instanceof Bravo) {
						MeterRegistry meterRegistry = context.getBean(MeterRegistry.class);
						meterRegistry.gauge("test", 1);
						System.out.println(meterRegistry.find("test").gauge().getId().getTags());
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
