/*
 * Copyright 2012-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.actuate.autoconfigure.metrics.export.wavefront;

import io.micrometer.core.instrument.Clock;
import io.micrometer.wavefront.WavefrontConfig;
import io.micrometer.wavefront.WavefrontMeterRegistry;
import org.junit.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link WavefrontMetricsExportAutoConfiguration}.
 *
 * @author Jon Schneider
 */
public class WavefrontMetricsExportAutoConfigurationTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withConfiguration(
					AutoConfigurations.of(WavefrontMetricsExportAutoConfiguration.class));

	@Test
	public void backsOffWithoutAClock() {
		this.contextRunner.run((context) -> assertThat(context)
				.doesNotHaveBean(WavefrontMeterRegistry.class));
	}

	@Test
	public void failsWithoutAnApiTokenWhenPublishingDirectly() {
		this.contextRunner.withUserConfiguration(BaseConfiguration.class)
				.run((context) -> assertThat(context).hasFailed());
	}

	@Test
	public void autoConfigurationCanBeDisabled() {
		this.contextRunner.withUserConfiguration(BaseConfiguration.class)
				.withPropertyValues("management.metrics.export.wavefront.enabled=false")
				.run((context) -> assertThat(context)
						.doesNotHaveBean(WavefrontMeterRegistry.class)
						.doesNotHaveBean(WavefrontConfig.class));
	}

	@Test
	public void allowsConfigToBeCustomized() {
		this.contextRunner.withUserConfiguration(CustomConfigConfiguration.class)
				.run((context) -> assertThat(context).hasSingleBean(Clock.class)
						.hasSingleBean(WavefrontMeterRegistry.class)
						.hasSingleBean(WavefrontConfig.class).hasBean("customConfig"));
	}

	@Test
	public void allowsRegistryToBeCustomized() {
		this.contextRunner.withUserConfiguration(CustomRegistryConfiguration.class)
				.withPropertyValues("management.metrics.export.wavefront.api-token=abcde")
				.run((context) -> assertThat(context).hasSingleBean(Clock.class)
						.hasSingleBean(WavefrontConfig.class)
						.hasSingleBean(WavefrontMeterRegistry.class)
						.hasBean("customRegistry"));
	}

	@Test
	public void stopsMeterRegistryWhenContextIsClosed() {
		this.contextRunner.withUserConfiguration(BaseConfiguration.class)
				.withPropertyValues("management.metrics.export.wavefront.api-token=abcde")
				.run((context) -> {
					WavefrontMeterRegistry registry = context
							.getBean(WavefrontMeterRegistry.class);
					assertThat(registry.isClosed()).isFalse();
					context.close();
					assertThat(registry.isClosed()).isTrue();
				});
	}

	@Configuration
	static class BaseConfiguration {

		@Bean
		public Clock clock() {
			return Clock.SYSTEM;
		}

	}

	@Configuration
	@Import(BaseConfiguration.class)
	static class CustomConfigConfiguration {

		@Bean
		public WavefrontConfig customConfig() {
			return new WavefrontConfig() {
				@Override
				public String get(String key) {
					return null;
				}

				@Override
				public String uri() {
					return WavefrontConfig.DEFAULT_PROXY.uri();
				}
			};
		}

	}

	@Configuration
	@Import(BaseConfiguration.class)
	static class CustomRegistryConfiguration {

		@Bean
		public WavefrontMeterRegistry customRegistry(WavefrontConfig config,
				Clock clock) {
			return new WavefrontMeterRegistry(config, clock);
		}

	}

}
