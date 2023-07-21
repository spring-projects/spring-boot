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

package org.springframework.boot.actuate.autoconfigure.metrics.redis;

import io.lettuce.core.metrics.MicrometerCommandLatencyRecorder;
import io.lettuce.core.metrics.MicrometerOptions;
import io.lettuce.core.resource.ClientResources;
import org.junit.jupiter.api.Test;

import org.springframework.boot.actuate.autoconfigure.metrics.test.MetricsRun;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link LettuceMetricsAutoConfiguration}.
 *
 * @author Antonin Arquey
 */
class LettuceMetricsAutoConfigurationTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(LettuceMetricsAutoConfiguration.class));

	@Test
	void whenThereIsAMeterRegistryThenCommandLatencyRecorderIsAdded() {
		this.contextRunner.with(MetricsRun.simple())
			.withConfiguration(AutoConfigurations.of(RedisAutoConfiguration.class))
			.run((context) -> {
				ClientResources clientResources = context.getBean(LettuceConnectionFactory.class).getClientResources();
				assertThat(clientResources.commandLatencyRecorder())
					.isInstanceOf(MicrometerCommandLatencyRecorder.class);
			});
	}

	@Test
	void autoConfiguredMicrometerOptionsUsesLettucesDefaults() {
		this.contextRunner.with(MetricsRun.simple())
			.withConfiguration(AutoConfigurations.of(RedisAutoConfiguration.class))
			.run((context) -> {
				MicrometerOptions micrometerOptions = context.getBean(MicrometerOptions.class);
				assertThat(micrometerOptions.isEnabled()).isTrue();
				assertThat(micrometerOptions.isHistogram()).isFalse();
				assertThat(micrometerOptions.localDistinction()).isFalse();
				assertThat(micrometerOptions.maxLatency()).isEqualTo(MicrometerOptions.DEFAULT_MAX_LATENCY);
				assertThat(micrometerOptions.minLatency()).isEqualTo(MicrometerOptions.DEFAULT_MIN_LATENCY);
			});
	}

	@Test
	void whenUserDefinesAMicrometerOptionsBeanThenCommandLatencyRecorderUsesIt() {
		this.contextRunner.with(MetricsRun.simple())
			.withConfiguration(AutoConfigurations.of(RedisAutoConfiguration.class))
			.withUserConfiguration(CustomMicrometerOptionsConfiguration.class)
			.run((context) -> {
				ClientResources clientResources = context.getBean(LettuceConnectionFactory.class).getClientResources();
				assertThat(clientResources.commandLatencyRecorder())
					.isInstanceOf(MicrometerCommandLatencyRecorder.class);
				assertThat(clientResources.commandLatencyRecorder()).hasFieldOrPropertyWithValue("options",
						context.getBean("customMicrometerOptions"));
			});
	}

	@Test
	void whenThereIsNoMeterRegistryThenClientResourcesCustomizationBacksOff() {
		this.contextRunner.withConfiguration(AutoConfigurations.of(RedisAutoConfiguration.class)).run((context) -> {
			ClientResources clientResources = context.getBean(LettuceConnectionFactory.class).getClientResources();
			assertThat(clientResources.commandLatencyRecorder())
				.isNotInstanceOf(MicrometerCommandLatencyRecorder.class);
		});
	}

	@Configuration(proxyBeanMethods = false)
	static class CustomMicrometerOptionsConfiguration {

		@Bean
		MicrometerOptions customMicrometerOptions() {
			return MicrometerOptions.create();
		}

	}

}
