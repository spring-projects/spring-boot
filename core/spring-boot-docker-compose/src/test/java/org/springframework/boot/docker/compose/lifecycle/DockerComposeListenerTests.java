/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.docker.compose.lifecycle;

import java.util.Set;

import org.jspecify.annotations.Nullable;
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

		private @Nullable DockerComposeLifecycleManager manager;

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

		@Nullable DockerComposeLifecycleManager getManager() {
			return this.manager;
		}

	}

}
