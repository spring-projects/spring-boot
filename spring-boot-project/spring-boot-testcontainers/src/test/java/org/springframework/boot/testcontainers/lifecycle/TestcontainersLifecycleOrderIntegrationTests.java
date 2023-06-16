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

package org.springframework.boot.testcontainers.lifecycle;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.ExtensionContext;

import org.springframework.boot.testcontainers.lifecycle.TestcontainersLifecycleOrderIntegrationTests.AssertingSpringExtension;
import org.springframework.boot.testcontainers.lifecycle.TestcontainersLifecycleOrderIntegrationTests.ContainerConfig;
import org.springframework.boot.testcontainers.lifecycle.TestcontainersLifecycleOrderIntegrationTests.TestConfig;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.testsupport.testcontainers.DisabledIfDockerUnavailable;
import org.springframework.boot.testsupport.testcontainers.RedisContainer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link TestcontainersLifecycleBeanPostProcessor} to ensure create
 * and destroy events happen in the correct order.
 *
 * @author Phillip Webb
 */
@ExtendWith(AssertingSpringExtension.class)
@ContextConfiguration(classes = { TestConfig.class, ContainerConfig.class })
@DirtiesContext
@DisabledIfDockerUnavailable
class TestcontainersLifecycleOrderIntegrationTests {

	static List<String> events = Collections.synchronizedList(new ArrayList<>());

	@Test
	void eventsAreOrderedCorrectlyAfterStartup() {
		assertThat(events).containsExactly("start-container", "create-bean");
	}

	@Configuration(proxyBeanMethods = false)
	static class ContainerConfig {

		@Bean
		@ServiceConnection("redis")
		RedisContainer redisContainer() {
			return new RedisContainer() {

				@Override
				public void start() {
					events.add("start-container");
					super.start();
				}

				@Override
				public void stop() {
					events.add("stop-container");
					super.stop();
				}

			};
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class TestConfig {

		@Bean
		TestBean testBean() {
			events.add("create-bean");
			return new TestBean();
		}

	}

	static class TestBean implements AutoCloseable {

		@Override
		public void close() throws Exception {
			events.add("destroy-bean");
		}

	}

	static class AssertingSpringExtension extends SpringExtension {

		@Override
		public void afterAll(ExtensionContext context) throws Exception {
			super.afterAll(context);
			assertThat(events).containsExactly("start-container", "create-bean", "destroy-bean", "stop-container");
		}

	}

}
