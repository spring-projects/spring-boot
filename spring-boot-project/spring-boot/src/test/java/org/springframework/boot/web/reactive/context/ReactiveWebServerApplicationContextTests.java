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

package org.springframework.boot.web.reactive.context;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.boot.availability.AvailabilityChangeEvent;
import org.springframework.boot.availability.ReadinessState;
import org.springframework.boot.web.context.ServerPortInfoApplicationContextInitializer;
import org.springframework.boot.web.reactive.server.MockReactiveWebServerFactory;
import org.springframework.context.ApplicationContextException;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.http.server.reactive.HttpHandler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.mockito.Mockito.verify;

/**
 * Tests for {@link ReactiveWebServerApplicationContext}.
 *
 * @author Andy Wilkinson
 */
class ReactiveWebServerApplicationContextTests {

	private ReactiveWebServerApplicationContext context = new ReactiveWebServerApplicationContext();

	@AfterEach
	void cleanUp() {
		this.context.close();
	}

	@Test
	void whenThereIsNoWebServerFactoryBeanThenContextRefreshWillFail() {
		assertThatExceptionOfType(ApplicationContextException.class).isThrownBy(() -> this.context.refresh())
				.withMessageContaining(
						"Unable to start ReactiveWebApplicationContext due to missing ReactiveWebServerFactory bean");
	}

	@Test
	void whenThereIsNoHttpHandlerBeanThenContextRefreshWillFail() {
		addWebServerFactoryBean();
		assertThatExceptionOfType(ApplicationContextException.class).isThrownBy(() -> this.context.refresh())
				.withMessageContaining("Unable to start ReactiveWebApplicationContext due to missing HttpHandler bean");
	}

	@Test
	void whenThereAreMultipleWebServerFactoryBeansThenContextRefreshWillFail() {
		addWebServerFactoryBean();
		addWebServerFactoryBean("anotherWebServerFactory");
		assertThatExceptionOfType(ApplicationContextException.class).isThrownBy(() -> this.context.refresh())
				.withMessageContaining(
						"Unable to start ReactiveWebApplicationContext due to multiple ReactiveWebServerFactory beans");
	}

	@Test
	void whenThereAreMultipleHttpHandlerBeansThenContextRefreshWillFail() {
		addWebServerFactoryBean();
		addHttpHandlerBean("httpHandler1");
		addHttpHandlerBean("httpHandler2");
		assertThatExceptionOfType(ApplicationContextException.class).isThrownBy(() -> this.context.refresh())
				.withMessageContaining(
						"Unable to start ReactiveWebApplicationContext due to multiple HttpHandler beans");
	}

	@Test
	void whenContextIsRefreshedThenReactiveWebServerInitializedEventIsPublished() {
		addWebServerFactoryBean();
		addHttpHandlerBean();
		TestApplicationListener listener = new TestApplicationListener();
		this.context.addApplicationListener(listener);
		this.context.refresh();
		List<ApplicationEvent> events = listener.receivedEvents();
		assertThat(events).hasSize(2).extracting("class").containsExactly(ReactiveWebServerInitializedEvent.class,
				ContextRefreshedEvent.class);
		ReactiveWebServerInitializedEvent initializedEvent = (ReactiveWebServerInitializedEvent) events.get(0);
		assertThat(initializedEvent.getSource().getPort()).isGreaterThanOrEqualTo(0);
		assertThat(initializedEvent.getApplicationContext()).isEqualTo(this.context);
	}

	@Test
	void whenContextIsRefreshedThenLocalServerPortIsAvailableFromTheEnvironment() {
		addWebServerFactoryBean();
		addHttpHandlerBean();
		new ServerPortInfoApplicationContextInitializer().initialize(this.context);
		this.context.refresh();
		ConfigurableEnvironment environment = this.context.getEnvironment();
		assertThat(environment.containsProperty("local.server.port")).isTrue();
		assertThat(environment.getProperty("local.server.port")).isEqualTo("8080");
	}

	@Test
	void whenContextIsClosedThenWebServerIsStopped() {
		addWebServerFactoryBean();
		addHttpHandlerBean();
		this.context.refresh();
		MockReactiveWebServerFactory factory = this.context.getBean(MockReactiveWebServerFactory.class);
		this.context.close();
		verify(factory.getWebServer()).stop();
	}

	@Test
	@SuppressWarnings("unchecked")
	void whenContextIsClosedThenApplicationAvailabilityChangesToRefusingTraffic() {
		addWebServerFactoryBean();
		addHttpHandlerBean();
		TestApplicationListener listener = new TestApplicationListener();
		this.context.refresh();
		this.context.addApplicationListener(listener);
		this.context.close();
		List<ApplicationEvent> events = listener.receivedEvents();
		assertThat(events).hasSize(2).extracting("class").contains(AvailabilityChangeEvent.class,
				ContextClosedEvent.class);
		assertThat(((AvailabilityChangeEvent<ReadinessState>) events.get(0)).getState())
				.isEqualTo(ReadinessState.REFUSING_TRAFFIC);
	}

	@Test
	void whenContextIsNotActiveThenCloseDoesNotChangeTheApplicationAvailability() {
		addWebServerFactoryBean();
		addHttpHandlerBean();
		TestApplicationListener listener = new TestApplicationListener();
		this.context.addApplicationListener(listener);
		this.context.registerBeanDefinition("refreshFailure", new RootBeanDefinition(RefreshFailure.class));
		assertThatExceptionOfType(BeanCreationException.class).isThrownBy(this.context::refresh);
		this.context.close();
		assertThat(listener.receivedEvents()).isEmpty();
	}

	@Test
	void whenTheContextIsRefreshedThenASubsequentRefreshAttemptWillFail() {
		addWebServerFactoryBean();
		addHttpHandlerBean();
		this.context.refresh();
		assertThatIllegalStateException().isThrownBy(() -> this.context.refresh())
				.withMessageContaining("multiple refresh attempts");
	}

	private void addHttpHandlerBean() {
		addHttpHandlerBean("httpHandler");
	}

	private void addHttpHandlerBean(String beanName) {
		this.context.registerBeanDefinition(beanName,
				new RootBeanDefinition(HttpHandler.class, () -> (request, response) -> null));
	}

	private void addWebServerFactoryBean() {
		addWebServerFactoryBean("webServerFactory");
	}

	private void addWebServerFactoryBean(String beanName) {
		this.context.registerBeanDefinition(beanName, new RootBeanDefinition(MockReactiveWebServerFactory.class));
	}

	static class TestApplicationListener implements ApplicationListener<ApplicationEvent> {

		private Deque<ApplicationEvent> events = new ArrayDeque<>();

		@Override
		public void onApplicationEvent(ApplicationEvent event) {
			this.events.add(event);
		}

		List<ApplicationEvent> receivedEvents() {
			List<ApplicationEvent> receivedEvents = new ArrayList<>();
			while (!this.events.isEmpty()) {
				receivedEvents.add(this.events.pollFirst());
			}
			return receivedEvents;
		}

	}

	static class RefreshFailure {

		RefreshFailure() {
			throw new RuntimeException("Fail refresh");
		}

	}

}
