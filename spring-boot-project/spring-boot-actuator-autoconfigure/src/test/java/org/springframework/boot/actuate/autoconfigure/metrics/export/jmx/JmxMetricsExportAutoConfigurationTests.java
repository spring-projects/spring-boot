/*
 * Copyright 2012-2019 the original author or authors.
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

package org.springframework.boot.actuate.autoconfigure.metrics.export.jmx;

import io.micrometer.core.instrument.Clock;
import io.micrometer.jmx.JmxConfig;
import io.micrometer.jmx.JmxMeterRegistry;
import org.junit.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link JmxMetricsExportAutoConfiguration}.
 *
 * @author Andy Wilkinson
 */
public class JmxMetricsExportAutoConfigurationTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(JmxMetricsExportAutoConfiguration.class));

	@Test
	public void backsOffWithoutAClock() {
		this.contextRunner.run((context) -> assertThat(context).doesNotHaveBean(JmxMeterRegistry.class));
	}

	@Test
	public void autoConfiguresItsConfigAndMeterRegistry() {
		this.contextRunner.withUserConfiguration(BaseConfiguration.class).run(
				(context) -> assertThat(context).hasSingleBean(JmxMeterRegistry.class).hasSingleBean(JmxConfig.class));
	}

	@Test
	public void autoConfigurationCanBeDisabled() {
		this.contextRunner.withUserConfiguration(BaseConfiguration.class)
				.withPropertyValues("management.metrics.export.jmx.enabled=false").run((context) -> assertThat(context)
						.doesNotHaveBean(JmxMeterRegistry.class).doesNotHaveBean(JmxConfig.class));
	}

	@Test
	public void allowsCustomConfigToBeUsed() {
		this.contextRunner.withUserConfiguration(CustomConfigConfiguration.class).run((context) -> assertThat(context)
				.hasSingleBean(JmxMeterRegistry.class).hasSingleBean(JmxConfig.class).hasBean("customConfig"));
	}

	@Test
	public void allowsCustomRegistryToBeUsed() {
		this.contextRunner.withUserConfiguration(CustomRegistryConfiguration.class).run((context) -> assertThat(context)
				.hasSingleBean(JmxMeterRegistry.class).hasBean("customRegistry").hasSingleBean(JmxConfig.class));
	}

	@Test
	public void stopsMeterRegistryWhenContextIsClosed() {
		this.contextRunner.withUserConfiguration(BaseConfiguration.class).run((context) -> {
			JmxMeterRegistry registry = context.getBean(JmxMeterRegistry.class);
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
		public JmxConfig customConfig() {
			return (key) -> null;
		}

	}

	@Configuration
	@Import(BaseConfiguration.class)
	static class CustomRegistryConfiguration {

		@Bean
		public JmxMeterRegistry customRegistry(JmxConfig config, Clock clock) {
			return new JmxMeterRegistry(config, clock);
		}

	}

}
