/*
 * Copyright 2012-2020 the original author or authors.
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

package org.springframework.boot.env;

import java.util.List;

import org.junit.jupiter.api.Test;

import org.springframework.boot.BootstrapRegistry;
import org.springframework.boot.DefaultBootstrapContext;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent;
import org.springframework.boot.context.event.ApplicationFailedEvent;
import org.springframework.boot.context.event.ApplicationPreparedEvent;
import org.springframework.boot.context.event.ApplicationStartingEvent;
import org.springframework.boot.logging.DeferredLogFactory;
import org.springframework.boot.logging.DeferredLogs;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

/**
 * Tests for {@link EnvironmentPostProcessorApplicationListener}.
 *
 * @author Phillip Webb
 */
class EnvironmentPostProcessorApplicationListenerTests {

	private DeferredLogs deferredLogs = spy(new DeferredLogs());

	private DefaultBootstrapContext bootstrapContext = spy(new DefaultBootstrapContext());

	private EnvironmentPostProcessorApplicationListener listener = new EnvironmentPostProcessorApplicationListener(
			EnvironmentPostProcessorsFactory.of(TestEnvironmentPostProcessor.class), this.deferredLogs);

	@Test
	void createUsesSpringFactories() {
		EnvironmentPostProcessorApplicationListener listener = new EnvironmentPostProcessorApplicationListener();
		assertThat(listener.getEnvironmentPostProcessors(this.bootstrapContext)).hasSizeGreaterThan(1);
	}

	@Test
	void createWhenHasFactoryUsesFactory() {
		EnvironmentPostProcessorApplicationListener listener = new EnvironmentPostProcessorApplicationListener(
				EnvironmentPostProcessorsFactory.of(TestEnvironmentPostProcessor.class));
		List<EnvironmentPostProcessor> postProcessors = listener.getEnvironmentPostProcessors(this.bootstrapContext);
		assertThat(postProcessors).hasSize(1);
		assertThat(postProcessors.get(0)).isInstanceOf(TestEnvironmentPostProcessor.class);
	}

	@Test
	void supportsEventTypeWhenApplicationEnvironmentPreparedEventReturnsTrue() {
		assertThat(this.listener.supportsEventType(ApplicationEnvironmentPreparedEvent.class)).isTrue();
	}

	@Test
	void supportsEventTypeWhenApplicationPreparedEventReturnsTrue() {
		assertThat(this.listener.supportsEventType(ApplicationPreparedEvent.class)).isTrue();
	}

	@Test
	void supportsEventTypeWhenApplicationFailedEventReturnsTrue() {
		assertThat(this.listener.supportsEventType(ApplicationFailedEvent.class)).isTrue();
	}

	@Test
	void supportsEventTypeWhenOtherEventReturnsFalse() {
		assertThat(this.listener.supportsEventType(ApplicationStartingEvent.class)).isFalse();
	}

	@Test
	void onApplicationEventWhenApplicationEnvironmentPreparedEventCallsPostProcessors() {
		SpringApplication application = mock(SpringApplication.class);
		MockEnvironment environment = new MockEnvironment();
		ApplicationEnvironmentPreparedEvent event = new ApplicationEnvironmentPreparedEvent(this.bootstrapContext,
				application, new String[0], environment);
		this.listener.onApplicationEvent(event);
		assertThat(environment.getProperty("processed")).isEqualTo("true");
	}

	@Test
	void onApplicationEventWhenApplicationPreparedEventSwitchesLogs() {
		SpringApplication application = mock(SpringApplication.class);
		ConfigurableApplicationContext context = mock(ConfigurableApplicationContext.class);
		ApplicationPreparedEvent event = new ApplicationPreparedEvent(application, new String[0], context);
		this.listener.onApplicationEvent(event);
		verify(this.deferredLogs).switchOverAll();
	}

	@Test
	void onApplicationEventWhenApplicationFailedEventSwitchesLogs() {
		SpringApplication application = mock(SpringApplication.class);
		ConfigurableApplicationContext context = mock(ConfigurableApplicationContext.class);
		ApplicationFailedEvent event = new ApplicationFailedEvent(application, new String[0], context,
				new RuntimeException());
		this.listener.onApplicationEvent(event);
		verify(this.deferredLogs).switchOverAll();
	}

	static class TestEnvironmentPostProcessor implements EnvironmentPostProcessor {

		TestEnvironmentPostProcessor(DeferredLogFactory logFactory, BootstrapRegistry bootstrapRegistry) {
			assertThat(logFactory).isNotNull();
			assertThat(bootstrapRegistry).isNotNull();
		}

		@Override
		public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
			((MockEnvironment) environment).setProperty("processed", "true");
		}

	}

}
