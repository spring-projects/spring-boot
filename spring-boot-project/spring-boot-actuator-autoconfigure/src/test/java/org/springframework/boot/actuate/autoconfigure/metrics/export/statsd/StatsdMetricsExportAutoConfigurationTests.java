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

package org.springframework.boot.actuate.autoconfigure.metrics.export.statsd;

import io.micrometer.core.instrument.Clock;
import io.micrometer.core.instrument.Meter.Id;
import io.micrometer.core.instrument.config.NamingConvention;
import io.micrometer.core.instrument.util.HierarchicalNameMapper;
import io.micrometer.statsd.StatsdConfig;
import io.micrometer.statsd.StatsdMeterRegistry;
import org.junit.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link StatsdMetricsExportAutoConfiguration}.
 *
 * @author Andy Wilkinson
 */
public class StatsdMetricsExportAutoConfigurationTests {

	private final ApplicationContextRunner runner = new ApplicationContextRunner()
			.withConfiguration(
					AutoConfigurations.of(StatsdMetricsExportAutoConfiguration.class));

	@Test
	public void backsOffWithoutAClock() {
		this.runner.run((context) -> assertThat(context)
				.doesNotHaveBean(StatsdMeterRegistry.class));
	}

	@Test
	public void autoConfiguresItsConfigMeterRegistryAndNameMapper() {
		this.runner.withUserConfiguration(BaseConfiguration.class)
				.run((context) -> assertThat(context)
						.hasSingleBean(StatsdMeterRegistry.class)
						.hasSingleBean(StatsdConfig.class)
						.hasSingleBean(HierarchicalNameMapper.class));
	}

	@Test
	public void allowsCustomConfigToBeUsed() {
		this.runner.withUserConfiguration(CustomConfigConfiguration.class)
				.run((context) -> assertThat(context)
						.hasSingleBean(StatsdMeterRegistry.class)
						.hasSingleBean(StatsdConfig.class).hasBean("customConfig")
						.hasSingleBean(HierarchicalNameMapper.class));
	}

	@Test
	public void allowsCustomRegistryToBeUsed() {
		this.runner.withUserConfiguration(CustomRegistryConfiguration.class)
				.run((context) -> assertThat(context)
						.hasSingleBean(StatsdMeterRegistry.class)
						.hasBean("customRegistry").hasSingleBean(StatsdConfig.class)
						.hasSingleBean(HierarchicalNameMapper.class));
	}

	@Test
	public void allowsCustomHierarchicalNameMapperToBeUsed() {
		this.runner.withUserConfiguration(CustomNameMapperConfiguration.class)
				.run((context) -> assertThat(context)
						.hasSingleBean(StatsdMeterRegistry.class)
						.hasSingleBean(StatsdConfig.class).hasBean("customNameMapper")
						.hasSingleBean(HierarchicalNameMapper.class));
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
		public StatsdConfig customConfig() {
			return new StatsdConfig() {

				@Override
				public String get(String k) {
					return null;
				}

			};
		}

	}

	@Configuration
	@Import(BaseConfiguration.class)
	static class CustomRegistryConfiguration {

		@Bean
		public StatsdMeterRegistry customRegistry(StatsdConfig config, Clock clock) {
			return new StatsdMeterRegistry(config, clock);
		}

	}

	@Configuration
	@Import(BaseConfiguration.class)
	static class CustomNameMapperConfiguration {

		@Bean
		public HierarchicalNameMapper customNameMapper() {
			return new HierarchicalNameMapper() {

				@Override
				public String toHierarchicalName(Id id, NamingConvention convention) {
					return "test";
				}

			};
		}

	}

}
