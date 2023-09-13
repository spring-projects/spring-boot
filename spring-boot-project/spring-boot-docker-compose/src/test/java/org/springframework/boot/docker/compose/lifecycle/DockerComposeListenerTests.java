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

package org.springframework.boot.docker.compose.lifecycle;

import java.util.Set;

import org.junit.jupiter.api.Test;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.SpringApplicationShutdownHandlers;
import org.springframework.boot.context.event.ApplicationPreparedEvent;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.context.ApplicationListener;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link DockerComposeListener}.
 *
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 * @author Phillip Webb
 */
class DockerComposeListenerTests {

	@Test
	void onApplicationPreparedEventCreatesAndStartsDockerComposeLifecycleManager() {
		SpringApplicationShutdownHandlers shutdownHandlers = mock(SpringApplicationShutdownHandlers.class);
		SpringApplication application = mock(SpringApplication.class);
		ConfigurableApplicationContext context = mock(ConfigurableApplicationContext.class);
		MockEnvironment environment = new MockEnvironment();
		given(context.getEnvironment()).willReturn(environment);
		TestDockerComposeListener listener = new TestDockerComposeListener(shutdownHandlers, context);
		ApplicationPreparedEvent event = new ApplicationPreparedEvent(application, new String[0], context);
		listener.onApplicationEvent(event);
		assertThat(listener.getManager()).isNotNull();
		then(listener.getManager()).should().start();
	}

	static class TestDockerComposeListener extends DockerComposeListener {

		private final ConfigurableApplicationContext context;

		private DockerComposeLifecycleManager manager;

		TestDockerComposeListener(SpringApplicationShutdownHandlers shutdownHandlers,
				ConfigurableApplicationContext context) {
			super(shutdownHandlers);
			this.context = context;
		}

		@Override
		protected DockerComposeLifecycleManager createDockerComposeLifecycleManager(
				ConfigurableApplicationContext applicationContext, Binder binder, DockerComposeProperties properties,
				Set<ApplicationListener<?>> eventListeners) {
			this.manager = mock(DockerComposeLifecycleManager.class);
			assertThat(applicationContext).isSameAs(this.context);
			assertThat(binder).isNotNull();
			assertThat(properties).isNotNull();
			return this.manager;
		}

		DockerComposeLifecycleManager getManager() {
			return this.manager;
		}

	}

}
