/*
 * Copyright 2012-2021 the original author or authors.
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

import java.util.concurrent.LinkedBlockingQueue;

import com.wavefront.sdk.common.WavefrontSender;
import io.micrometer.core.instrument.Clock;
import io.micrometer.wavefront.WavefrontConfig;
import io.micrometer.wavefront.WavefrontMeterRegistry;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import static org.assertj.core.api.Assertions.as;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link WavefrontMetricsExportAutoConfiguration}.
 *
 * @author Jon Schneider
 * @author Stephane Nicoll
 */
class WavefrontMetricsExportAutoConfigurationTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(WavefrontMetricsExportAutoConfiguration.class));

	@Test
	void backsOffWithoutAClock() {
		this.contextRunner.run((context) -> assertThat(context).doesNotHaveBean(WavefrontMeterRegistry.class));
	}

	@Test
	void failsWithoutAnApiTokenWhenPublishingDirectly() {
		this.contextRunner.withUserConfiguration(BaseConfiguration.class)
				.run((context) -> assertThat(context).hasFailed());
	}

	@Test
	void autoConfigurationCanBeDisabledWithDefaultsEnabledProperty() {
		this.contextRunner.withUserConfiguration(BaseConfiguration.class)
				.withPropertyValues("management.metrics.export.wavefront.api-token=abcde",
						"management.metrics.export.defaults.enabled=false")
				.run((context) -> assertThat(context).doesNotHaveBean(WavefrontMeterRegistry.class)
						.doesNotHaveBean(WavefrontConfig.class).doesNotHaveBean(WavefrontSender.class));
	}

	@Test
	void autoConfigurationCanBeDisabledWithSpecificEnabledProperty() {
		this.contextRunner.withUserConfiguration(BaseConfiguration.class)
				.withPropertyValues("management.metrics.export.wavefront.api-token=abcde",
						"management.metrics.export.wavefront.enabled=false")
				.run((context) -> assertThat(context).doesNotHaveBean(WavefrontMeterRegistry.class)
						.doesNotHaveBean(WavefrontConfig.class).doesNotHaveBean(WavefrontSender.class));
	}

	@Test
	void allowsConfigToBeCustomized() {
		this.contextRunner.withUserConfiguration(CustomConfigConfiguration.class)
				.run((context) -> assertThat(context).hasSingleBean(Clock.class)
						.hasSingleBean(WavefrontMeterRegistry.class).hasSingleBean(WavefrontConfig.class)
						.hasSingleBean(WavefrontSender.class).hasBean("customConfig"));
	}

	@Test
	void defaultWavefrontSenderSettingsAreConsistent() {
		this.contextRunner.withUserConfiguration(BaseConfiguration.class)
				.withPropertyValues("management.metrics.export.wavefront.api-token=abcde").run((context) -> {
					WavefrontProperties properties = new WavefrontProperties();
					WavefrontSender sender = context.getBean(WavefrontSender.class);
					assertThat(sender)
							.extracting("metricsBuffer", as(InstanceOfAssertFactories.type(LinkedBlockingQueue.class)))
							.satisfies((queue) -> assertThat(queue.remainingCapacity() + queue.size())
									.isEqualTo(properties.getSender().getMaxQueueSize()));
					assertThat(sender).hasFieldOrPropertyWithValue("batchSize", properties.getBatchSize());
					assertThat(sender).hasFieldOrPropertyWithValue("messageSizeBytes",
							(int) properties.getSender().getMessageSize().toBytes());
				});
	}

	@Test
	void configureWavefrontSender() {
		this.contextRunner.withUserConfiguration(BaseConfiguration.class)
				.withPropertyValues("management.metrics.export.wavefront.api-token=abcde",
						"management.metrics.export.wavefront.batch-size=50",
						"management.metrics.export.wavefront.sender.max-queue-size=100",
						"management.metrics.export.wavefront.sender.message-size=1KB")
				.run((context) -> {
					WavefrontSender sender = context.getBean(WavefrontSender.class);
					assertThat(sender).hasFieldOrPropertyWithValue("batchSize", 50);
					assertThat(sender)
							.extracting("metricsBuffer", as(InstanceOfAssertFactories.type(LinkedBlockingQueue.class)))
							.satisfies((queue) -> assertThat(queue.remainingCapacity() + queue.size()).isEqualTo(100));
					assertThat(sender).hasFieldOrPropertyWithValue("messageSizeBytes", 1024);
				});
	}

	@Test
	void allowsWavefrontSenderToBeCustomized() {
		this.contextRunner.withUserConfiguration(CustomSenderConfiguration.class)
				.run((context) -> assertThat(context).hasSingleBean(Clock.class)
						.hasSingleBean(WavefrontMeterRegistry.class).hasSingleBean(WavefrontConfig.class)
						.hasSingleBean(WavefrontSender.class).hasBean("customSender"));
	}

	@Test
	void allowsRegistryToBeCustomized() {
		this.contextRunner.withUserConfiguration(CustomRegistryConfiguration.class)
				.withPropertyValues("management.metrics.export.wavefront.api-token=abcde")
				.run((context) -> assertThat(context).hasSingleBean(Clock.class).hasSingleBean(WavefrontConfig.class)
						.hasSingleBean(WavefrontMeterRegistry.class).hasBean("customRegistry"));
	}

	@Test
	void stopsMeterRegistryWhenContextIsClosed() {
		this.contextRunner.withUserConfiguration(BaseConfiguration.class)
				.withPropertyValues("management.metrics.export.wavefront.api-token=abcde").run((context) -> {
					WavefrontMeterRegistry registry = context.getBean(WavefrontMeterRegistry.class);
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
