/*
 * Copyright 2012-2024 the original author or authors.
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.redis.testcontainers.RedisContainer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.testcontainers.utility.DockerImageName;

import org.springframework.beans.factory.ObjectFactory;
import org.springframework.boot.testcontainers.lifecycle.TestcontainersLifecycleOrderWithScopeIntegrationTests.AssertingSpringExtension;
import org.springframework.boot.testcontainers.lifecycle.TestcontainersLifecycleOrderWithScopeIntegrationTests.ContainerConfig;
import org.springframework.boot.testcontainers.lifecycle.TestcontainersLifecycleOrderWithScopeIntegrationTests.ScopedContextLoader;
import org.springframework.boot.testcontainers.lifecycle.TestcontainersLifecycleOrderWithScopeIntegrationTests.TestConfig;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.testsupport.container.DisabledIfDockerUnavailable;
import org.springframework.boot.testsupport.container.TestImage;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.support.AnnotationConfigContextLoader;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link TestcontainersLifecycleBeanPostProcessor} to ensure create
 * and destroy events happen in the correct order.
 *
 * @author Phillip Webb
 */
@ExtendWith(AssertingSpringExtension.class)
@ContextConfiguration(loader = ScopedContextLoader.class, classes = { TestConfig.class, ContainerConfig.class })
@DirtiesContext
@DisabledIfDockerUnavailable
class TestcontainersLifecycleOrderWithScopeIntegrationTests {

	static List<String> events = Collections.synchronizedList(new ArrayList<>());

	@Test
	void eventsAreOrderedCorrectlyAfterStartup() {
		assertThat(events).containsExactly("start-container", "create-bean");
	}

	@Configuration(proxyBeanMethods = false)
	static class ContainerConfig {

		@Bean
		@Scope("custom")
		@ServiceConnection
		RedisContainer redisContainer() {
			return TestImage.container(EventRecordingRedisContainer.class);
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

	static class EventRecordingRedisContainer extends RedisContainer {

		EventRecordingRedisContainer(DockerImageName dockerImageName) {
			super(dockerImageName);
		}

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

	}

	static class ScopedContextLoader extends AnnotationConfigContextLoader {

		@Override
		protected GenericApplicationContext createContext() {
			CustomScope customScope = new CustomScope();
			AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext() {

				@Override
				protected void onClose() {
					customScope.destroy();
					super.onClose();
				}

			};
			context.getBeanFactory().registerScope("custom", customScope);
			return context;
		}

	}

	static class CustomScope implements org.springframework.beans.factory.config.Scope {

		private Map<String, Object> instances = new HashMap<>();

		private MultiValueMap<String, Runnable> destructors = new LinkedMultiValueMap<>();

		@Override
		public Object get(String name, ObjectFactory<?> objectFactory) {
			return this.instances.computeIfAbsent(name, (key) -> objectFactory.getObject());
		}

		@Override
		public Object remove(String name) {
			synchronized (this) {
				Object removed = this.instances.remove(name);
				this.destructors.get(name).forEach(Runnable::run);
				this.destructors.remove(name);
				return removed;
			}
		}

		@Override
		public void registerDestructionCallback(String name, Runnable callback) {
			this.destructors.add(name, callback);
		}

		@Override
		public Object resolveContextualObject(String key) {
			return null;
		}

		@Override
		public String getConversationId() {
			return null;
		}

		void destroy() {
			synchronized (this) {
				this.destructors.forEach((name, actions) -> actions.forEach(Runnable::run));
				this.destructors.clear();
				this.instances.clear();
			}
		}

	}

}
