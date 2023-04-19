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

package org.springframework.boot.testcontainers.service.connection;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.autoconfigure.data.redis.RedisConnectionDetails;
import org.springframework.boot.testcontainers.lifecycle.TestcontainersLifecycleApplicationContextInitializer;
import org.springframework.boot.testsupport.testcontainers.DisabledIfDockerUnavailable;
import org.springframework.boot.testsupport.testcontainers.RedisContainer;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link ServiceConnectionAutoConfiguration}.
 *
 * @author Phillip Webb
 */
@DisabledIfDockerUnavailable
class ServiceConnectionAutoConfigurationTests {

	private static final String REDIS_CONTAINER_CONNECTION_DETAILS = "org.springframework.boot.testcontainers.service.connection.redis."
			+ "RedisContainerConnectionDetailsFactory$RedisContainerConnectionDetails";

	@Test
	void whenNoExistingBeansRegistersServiceConnection() {
		try (AnnotationConfigApplicationContext applicationContext = new AnnotationConfigApplicationContext()) {
			applicationContext.register(WithNoExtraAutoConfiguration.class, ContainerConfiguration.class);
			new TestcontainersLifecycleApplicationContextInitializer().initialize(applicationContext);
			applicationContext.refresh();
			RedisConnectionDetails connectionDetails = applicationContext.getBean(RedisConnectionDetails.class);
			assertThat(connectionDetails.getClass().getName()).isEqualTo(REDIS_CONTAINER_CONNECTION_DETAILS);
		}
	}

	@Test
	void whenHasExistingAutoConfigurationRegistersReplacement() {
		try (AnnotationConfigApplicationContext applicationContext = new AnnotationConfigApplicationContext()) {
			applicationContext.register(WithRedisAutoConfiguration.class, ContainerConfiguration.class);
			new TestcontainersLifecycleApplicationContextInitializer().initialize(applicationContext);
			applicationContext.refresh();
			RedisConnectionDetails connectionDetails = applicationContext.getBean(RedisConnectionDetails.class);
			assertThat(connectionDetails.getClass().getName()).isEqualTo(REDIS_CONTAINER_CONNECTION_DETAILS);
		}
	}

	@Test
	void whenHasUserConfigurationDoesNotRegisterReplacement() {
		try (AnnotationConfigApplicationContext applicationContext = new AnnotationConfigApplicationContext()) {
			applicationContext.register(UserConfiguration.class, WithRedisAutoConfiguration.class,
					ContainerConfiguration.class);
			new TestcontainersLifecycleApplicationContextInitializer().initialize(applicationContext);
			applicationContext.refresh();
			RedisConnectionDetails connectionDetails = applicationContext.getBean(RedisConnectionDetails.class);
			assertThat(Mockito.mockingDetails(connectionDetails).isMock()).isTrue();
		}
	}

	@Configuration(proxyBeanMethods = false)
	@ImportAutoConfiguration(ServiceConnectionAutoConfiguration.class)
	static class WithNoExtraAutoConfiguration {

	}

	@Configuration(proxyBeanMethods = false)
	@ImportAutoConfiguration({ ServiceConnectionAutoConfiguration.class, RedisAutoConfiguration.class })
	static class WithRedisAutoConfiguration {

	}

	@Configuration(proxyBeanMethods = false)
	static class ContainerConfiguration {

		@Bean
		@ServiceConnection
		RedisContainer redisContainer() {
			return new RedisContainer();
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class UserConfiguration {

		@Bean
		RedisConnectionDetails redisConnectionDetails() {
			return mock(RedisConnectionDetails.class);
		}

	}

}
