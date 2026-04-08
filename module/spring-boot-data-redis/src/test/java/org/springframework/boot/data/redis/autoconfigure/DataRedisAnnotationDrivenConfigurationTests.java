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

package org.springframework.boot.data.redis.autoconfigure;

import java.time.Duration;

import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.data.redis.autoconfigure.DataRedisProperties.Listener;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.config.RedisListenerConfigUtils;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.util.backoff.ExponentialBackOff;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link DataRedisAnnotationDrivenConfiguration}.
 *
 * @author Stephane Nicoll
 */
class DataRedisAnnotationDrivenConfigurationTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(DataRedisAnnotationDrivenConfiguration.class))
		.withUserConfiguration(TestConfiguration.class);

	@Test
	void registersContainerAndAnnotationProcessor() {
		this.contextRunner.run((context) -> {
			assertThat(context).hasSingleBean(RedisMessageListenerContainer.class);
			assertThat(context).hasBean(RedisListenerConfigUtils.REDIS_LISTENER_ANNOTATION_PROCESSOR_BEAN_NAME);
		});
	}

	@Test
	void backsOffWhenContainerWithDefaultNameIsDefined() {
		RedisMessageListenerContainer container = mock(RedisMessageListenerContainer.class);
		this.contextRunner
			.withBean("redisMessageListenerContainer", RedisMessageListenerContainer.class, () -> container)
			.run((context) -> assertThat(context).hasSingleBean(RedisMessageListenerContainer.class)
				.getBean(RedisMessageListenerContainer.class)
				.isSameAs(container));
	}

	@Test
	void registerContainerWhenContainerWithCustomNameIsDefined() {
		RedisMessageListenerContainer container = mock(RedisMessageListenerContainer.class);
		this.contextRunner
			.withBean("customRedisMessageListenerContainer", RedisMessageListenerContainer.class, () -> container)
			.run((context) -> assertThat(context).getBeans(RedisMessageListenerContainer.class)
				.hasSize(2)
				.containsKey("customRedisMessageListenerContainer"));
	}

	@Test
	void containerConfigurationMatchesDefaults() {
		this.contextRunner.run((context) -> {
			RedisMessageListenerContainer container = context.getBean(RedisMessageListenerContainer.class);
			Listener listener = new DataRedisProperties().getListener();
			assertThat(container.isAutoStartup()).isEqualTo(listener.isAutoStartup());
			assertThat(container.getMaxSubscriptionRegistrationWaitingTime())
				.isEqualTo(listener.getSubscriptionRegistrationTimeout().toMillis());
		});
	}

	@Test
	void containerCanBeConfigured() {
		this.contextRunner.withPropertyValues("spring.data.redis.listener.auto-startup=false",
				"spring.data.redis.listener.max-subscription-registration-waiting-time=2s",
				"spring.data.redis.listener.recovery.max-retries=6", "spring.data.redis.listener.recovery.delay=4s",
				"spring.data.redis.listener.recovery.multiplier=1.5",
				"spring.data.redis.listener.recovery.max-delay=2m", "spring.data.redis.listener.recovery.jitter=500ms")
			.run((context) -> {
				RedisMessageListenerContainer container = context.getBean(RedisMessageListenerContainer.class);
				assertThat(container.isAutoStartup()).isFalse();
				assertThat(container.getMaxSubscriptionRegistrationWaitingTime()).isEqualTo(2000);
				assertThat(container).extracting("backOff")
					.asInstanceOf(InstanceOfAssertFactories.type(ExponentialBackOff.class))
					.satisfies((backOff) -> {
						assertThat(backOff.getMaxAttempts()).isEqualTo(6);
						assertThat(backOff.getInitialInterval()).isEqualTo(4000);
						assertThat(backOff.getMultiplier()).isEqualTo(1.5);
						assertThat(backOff.getMaxInterval()).isEqualTo(Duration.ofMinutes(2).toMillis());
						assertThat(backOff.getJitter()).isEqualTo(500);
					});
			});
	}

	@Configuration(proxyBeanMethods = false)
	@EnableConfigurationProperties(DataRedisProperties.class)
	static class TestConfiguration {

		@Bean
		RedisConnectionFactory redisConnectionFactory() {
			return mock(RedisConnectionFactory.class);
		}

	}

}
