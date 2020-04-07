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

package org.springframework.boot.actuate.autoconfigure.metrics.export.graphite;

import io.micrometer.core.instrument.Clock;
import io.micrometer.core.instrument.Tags;
import io.micrometer.graphite.GraphiteConfig;
import io.micrometer.graphite.GraphiteMeterRegistry;
import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link GraphiteMetricsExportAutoConfiguration}.
 *
 * @author Andy Wilkinson
 * @author Stephane Nicoll
 */
class GraphiteMetricsExportAutoConfigurationTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(GraphiteMetricsExportAutoConfiguration.class));

	@Test
	void backsOffWithoutAClock() {
		this.contextRunner.run((context) -> assertThat(context).doesNotHaveBean(GraphiteMeterRegistry.class));
	}

	@Test
	void autoConfiguresUseTagsAsPrefixIsIgnoredByDefault() {
		this.contextRunner.withUserConfiguration(BaseConfiguration.class)
				.withPropertyValues("management.metrics.export.graphite.tags-as-prefix=ignored").run((context) -> {
					assertThat(context).hasSingleBean(GraphiteMeterRegistry.class);
					GraphiteMeterRegistry registry = context.getBean(GraphiteMeterRegistry.class);
					registry.counter("test.count", Tags.of("app", "myapp"));
					assertThat(registry.getDropwizardRegistry().getMeters()).containsOnlyKeys("test.count;app=myapp");
				});
	}

	@Test
	void autoConfiguresUseTagsAsPrefix() {
		this.contextRunner.withUserConfiguration(BaseConfiguration.class)
				.withPropertyValues("management.metrics.export.graphite.tags-as-prefix=app",
						"management.metrics.export.graphite.graphite-tags-enabled=false")
				.run((context) -> {
					assertThat(context).hasSingleBean(GraphiteMeterRegistry.class);
					GraphiteMeterRegistry registry = context.getBean(GraphiteMeterRegistry.class);
					registry.counter("test.count", Tags.of("app", "myapp"));
					assertThat(registry.getDropwizardRegistry().getMeters()).containsOnlyKeys("myapp.testCount");
				});
	}

	@Test
	void autoConfiguresItsConfigAndMeterRegistry() {
		this.contextRunner.withUserConfiguration(BaseConfiguration.class).run((context) -> assertThat(context)
				.hasSingleBean(GraphiteMeterRegistry.class).hasSingleBean(GraphiteConfig.class));
	}

	@Test
	void autoConfigurationCanBeDisabled() {
		this.contextRunner.withUserConfiguration(BaseConfiguration.class)
				.withPropertyValues("management.metrics.export.graphite.enabled=false")
				.run((context) -> assertThat(context).doesNotHaveBean(GraphiteMeterRegistry.class)
						.doesNotHaveBean(GraphiteConfig.class));
	}

	@Test
	void allowsCustomConfigToBeUsed() {
		this.contextRunner.withUserConfiguration(CustomConfigConfiguration.class)
				.run((context) -> assertThat(context).hasSingleBean(GraphiteMeterRegistry.class)
						.hasSingleBean(GraphiteConfig.class).hasBean("customConfig"));
	}

	@Test
	void allowsCustomRegistryToBeUsed() {
		this.contextRunner.withUserConfiguration(CustomRegistryConfiguration.class)
				.run((context) -> assertThat(context).hasSingleBean(GraphiteMeterRegistry.class)
						.hasBean("customRegistry").hasSingleBean(GraphiteConfig.class));
	}

	@Test
	void stopsMeterRegistryWhenContextIsClosed() {
		this.contextRunner.withUserConfiguration(BaseConfiguration.class).run((context) -> {
			GraphiteMeterRegistry registry = context.getBean(GraphiteMeterRegistry.class);
			assertThat(registry.isClosed()).isFalse();
			context.close();
			assertThat(registry.isClosed()).isTrue();
		});
	}

	@Configuration(proxyBeanMethods = false)
	static class BaseConfiguration {

		@Bean
		Clock clock() {
			return Clock.SYSTEM;
		}

	}

	@Configuration(proxyBeanMethods = false)
	@Import(BaseConfiguration.class)
	static class CustomConfigConfiguration {

		@Bean
		GraphiteConfig customConfig() {
			return (key) -> {
				if ("Graphite.apiKey".equals(key)) {
					return "12345";
				}
				return null;
			};
		}

	}

	@Configuration(proxyBeanMethods = false)
	@Import(BaseConfiguration.class)
	static class CustomRegistryConfiguration {

		@Bean
		GraphiteMeterRegistry customRegistry(GraphiteConfig config, Clock clock) {
			return new GraphiteMeterRegistry(config, clock);
		}

	}

}
