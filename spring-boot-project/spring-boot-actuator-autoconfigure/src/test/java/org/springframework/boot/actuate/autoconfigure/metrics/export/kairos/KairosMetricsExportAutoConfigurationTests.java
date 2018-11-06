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

package org.springframework.boot.actuate.autoconfigure.metrics.export.kairos;

import io.micrometer.core.instrument.Clock;
import io.micrometer.kairos.KairosConfig;
import io.micrometer.kairos.KairosMeterRegistry;
import org.junit.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link KairosMetricsExportAutoConfiguration}.
 *
 * @author Stephane Nicoll
 */
public class KairosMetricsExportAutoConfigurationTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withConfiguration(
					AutoConfigurations.of(KairosMetricsExportAutoConfiguration.class));

	@Test
	public void backsOffWithoutAClock() {
		this.contextRunner.run((context) -> assertThat(context)
				.doesNotHaveBean(KairosMeterRegistry.class));
	}

	@Test
	public void autoConfiguresItsConfigAndMeterRegistry() {
		this.contextRunner.withUserConfiguration(BaseConfiguration.class)
				.run((context) -> assertThat(context)
						.hasSingleBean(KairosMeterRegistry.class)
						.hasSingleBean(KairosConfig.class));
	}

	@Test
	public void autoConfigurationCanBeDisabled() {
		this.contextRunner.withUserConfiguration(BaseConfiguration.class)
				.withPropertyValues("management.metrics.export.kairos.enabled=false")
				.run((context) -> assertThat(context)
						.doesNotHaveBean(KairosMeterRegistry.class)
						.doesNotHaveBean(KairosConfig.class));
	}

	@Test
	public void allowsCustomConfigToBeUsed() {
		this.contextRunner.withUserConfiguration(CustomConfigConfiguration.class)
				.run((context) -> assertThat(context)
						.hasSingleBean(KairosMeterRegistry.class)
						.hasSingleBean(KairosConfig.class).hasBean("customConfig"));
	}

	@Test
	public void allowsCustomRegistryToBeUsed() {
		this.contextRunner.withUserConfiguration(CustomRegistryConfiguration.class)
				.run((context) -> assertThat(context)
						.hasSingleBean(KairosMeterRegistry.class)
						.hasBean("customRegistry").hasSingleBean(KairosConfig.class));
	}

	@Test
	public void stopsMeterRegistryWhenContextIsClosed() {
		this.contextRunner.withUserConfiguration(BaseConfiguration.class)
				.run((context) -> {
					KairosMeterRegistry registry = context
							.getBean(KairosMeterRegistry.class);
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
		public KairosConfig customConfig() {
			return (key) -> null;
		}

	}

	@Configuration
	@Import(BaseConfiguration.class)
	static class CustomRegistryConfiguration {

		@Bean
		public KairosMeterRegistry customRegistry(KairosConfig config, Clock clock) {
			return new KairosMeterRegistry(config, clock);
		}

	}

}
