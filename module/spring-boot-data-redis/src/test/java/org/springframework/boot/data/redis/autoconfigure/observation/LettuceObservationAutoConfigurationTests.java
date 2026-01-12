/*
 * Copyright 2012-present the original author or authors.
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

package org.springframework.boot.data.redis.autoconfigure.observation;

import io.lettuce.core.resource.ClientResources;
import io.lettuce.core.tracing.MicrometerTracing;
import io.micrometer.observation.tck.TestObservationRegistry;
import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.data.redis.autoconfigure.DataRedisAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link LettuceObservationAutoConfiguration}.
 *
 * @author Antonin Arquey
 * @author Stephane Nicoll
 */
class LettuceObservationAutoConfigurationTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(LettuceObservationAutoConfiguration.class));

	@Test
	void whenThereIsAnObservationRegistryThenMicrometerTracingIsAdded() {
		this.contextRunner.withBean(TestObservationRegistry.class, TestObservationRegistry::create)
			.withConfiguration(AutoConfigurations.of(DataRedisAutoConfiguration.class))
			.run((context) -> {
				ClientResources clientResources = context.getBean(LettuceConnectionFactory.class).getClientResources();
				assertThat(clientResources).isNotNull();
				assertThat(clientResources.tracing()).isInstanceOf(MicrometerTracing.class);
			});

	}

	@Test
	void whenThereIsNoObservationRegistryThenClientResourcesCustomizationBacksOff() {
		this.contextRunner.withConfiguration(AutoConfigurations.of(DataRedisAutoConfiguration.class)).run((context) -> {
			ClientResources clientResources = context.getBean(LettuceConnectionFactory.class).getClientResources();
			assertThat(clientResources).isNotNull();
			assertThat(clientResources.tracing()).isNotInstanceOf(MicrometerTracing.class);
		});
	}

}
