/*
 * Copyright 2012-2023 the original author or authors.
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

package org.springframework.boot.actuate.autoconfigure.metrics.export.wavefront;

import java.util.Map;

import com.wavefront.sdk.common.WavefrontSender;
import com.wavefront.sdk.common.application.ApplicationTags;
import io.micrometer.core.instrument.Clock;
import io.micrometer.wavefront.WavefrontConfig;
import io.micrometer.wavefront.WavefrontMeterRegistry;
import org.junit.jupiter.api.Test;

import org.springframework.boot.actuate.autoconfigure.metrics.MetricsAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.wavefront.WavefrontAutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link WavefrontMetricsExportAutoConfiguration}.
 *
 * @author Jon Schneider
 * @author Stephane Nicoll
 * @author Glenn Oppegard
 */
class WavefrontMetricsExportAutoConfigurationTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner().withConfiguration(
			AutoConfigurations.of(WavefrontAutoConfiguration.class, WavefrontMetricsExportAutoConfiguration.class));

	@Test
	void backsOffWithoutAClock() {
		this.contextRunner.run((context) -> assertThat(context).doesNotHaveBean(WavefrontMeterRegistry.class));
	}

	@Test
	void autoConfigurationCanBeDisabledWithDefaultsEnabledProperty() {
		this.contextRunner.withUserConfiguration(BaseConfiguration.class)
			.withPropertyValues("management.wavefront.api-token=abcde",
					"management.defaults.metrics.export.enabled=false")
			.run((context) -> assertThat(context).doesNotHaveBean(WavefrontMeterRegistry.class)
				.doesNotHaveBean(WavefrontConfig.class));
	}

	@Test
	void autoConfigurationCanBeDisabledWithSpecificEnabledProperty() {
		this.contextRunner.withUserConfiguration(BaseConfiguration.class)
			.withPropertyValues("management.wavefront.api-token=abcde",
					"management.wavefront.metrics.export.enabled=false")
			.run((context) -> assertThat(context).doesNotHaveBean(WavefrontMeterRegistry.class)
				.doesNotHaveBean(WavefrontConfig.class));
	}

	@Test
	void allowsConfigToBeCustomized() {
		this.contextRunner.withUserConfiguration(CustomConfigConfiguration.class)
			.run((context) -> assertThat(context).hasSingleBean(Clock.class)
				.hasSingleBean(WavefrontMeterRegistry.class)
				.hasSingleBean(WavefrontConfig.class)
				.hasSingleBean(WavefrontSender.class)
				.hasBean("customConfig"));
	}

	@Test
	void allowsRegistryToBeCustomized() {
		this.contextRunner.withUserConfiguration(CustomRegistryConfiguration.class)
			.withPropertyValues("management.wavefront.api-token=abcde")
			.run((context) -> assertThat(context).hasSingleBean(Clock.class)
				.hasSingleBean(WavefrontConfig.class)
				.hasSingleBean(WavefrontMeterRegistry.class)
				.hasBean("customRegistry"));
	}

	@Test
	void exportsApplicationTagsInWavefrontRegistryWhenApplicationTagsBean() {
		ApplicationTags.Builder builder = new ApplicationTags.Builder("super-application", "super-service");
		builder.cluster("super-cluster");
		builder.shard("super-shard");
		builder.customTags(Map.of("custom-key", "custom-val"));
		this.contextRunner.withConfiguration(AutoConfigurations.of(MetricsAutoConfiguration.class))
			.withUserConfiguration(BaseConfiguration.class)
			.withBean(ApplicationTags.class, builder::build)
			.run((context) -> {
				WavefrontMeterRegistry registry = context.getBean(WavefrontMeterRegistry.class);
				registry.counter("my.counter", "env", "qa");
				assertThat(registry.find("my.counter")
					.tags("env", "qa")
					.tags("application", "super-application")
					.tags("service", "super-service")
					.tags("cluster", "super-cluster")
					.tags("shard", "super-shard")
					.tags("custom-key", "custom-val")
					.counter()).isNotNull();
			});
	}

	@Test
	void exportsApplicationTagsInWavefrontRegistryWhenInProperties() {
		this.contextRunner.withConfiguration(AutoConfigurations.of(MetricsAutoConfiguration.class))
			.withPropertyValues("management.wavefront.application.service-name=super-service",
					"management.wavefront.application.name=super-application",
					"management.wavefront.application.cluster-name=super-cluster",
					"management.wavefront.application.shard-name=super-shard")
			.withUserConfiguration(BaseConfiguration.class)
			.run((context) -> {
				WavefrontMeterRegistry registry = context.getBean(WavefrontMeterRegistry.class);
				registry.counter("my.counter", "env", "qa");
				assertThat(registry.find("my.counter")
					.tags("env", "qa")
					.tags("application", "super-application")
					.tags("service", "super-service")
					.tags("cluster", "super-cluster")
					.tags("shard", "super-shard")
					.counter()).isNotNull();
			});
	}

	@Test
	void stopsMeterRegistryWhenContextIsClosed() {
		this.contextRunner.withUserConfiguration(BaseConfiguration.class)
			.withPropertyValues("management.wavefront.api-token=abcde")
			.run((context) -> {
				WavefrontMeterRegistry registry = context.getBean(WavefrontMeterRegistry.class);
				assertThat(registry.isClosed()).isFalse();
				context.close();
				assertThat(registry.isClosed()).isTrue();
			});
	}

	@Configuration(proxyBeanMethods = false)
	static class BaseConfiguration {

		@Bean
		WavefrontSender customWavefrontSender() {
			return mock(WavefrontSender.class);
		}

		@Bean
		Clock clock() {
			return Clock.SYSTEM;
		}

	}

	@Configuration(proxyBeanMethods = false)
	@Import(BaseConfiguration.class)
	static class CustomConfigConfiguration {

		@Bean
		WavefrontConfig customConfig() {
			return new WavefrontConfig() {
				@Override
				public String get(String key) {
					return null;
				}

				@Override
				public String uri() {
					return WavefrontConfig.DEFAULT_DIRECT.uri();
				}

				@Override
				public String apiToken() {
					return "abc-def";
				}
			};
		}

	}

	@Configuration(proxyBeanMethods = false)
	@Import(BaseConfiguration.class)
	static class CustomSenderConfiguration {

		@Bean
		WavefrontSender customSender() {
			return mock(WavefrontSender.class);
		}

	}

	@Configuration(proxyBeanMethods = false)
	@Import(BaseConfiguration.class)
	static class CustomRegistryConfiguration {

		@Bean
		WavefrontMeterRegistry customRegistry(WavefrontConfig config, Clock clock) {
			return new WavefrontMeterRegistry(config, clock);
		}

	}

}
