/*
 * Copyright 2012-2019 the original author or authors.
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

package org.springframework.boot.context.config;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.support.StaticApplicationContext;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.test.context.support.TestPropertySourceUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link DelegatingApplicationListener}.
 *
 * @author Dave Syer
 */
class DelegatingApplicationListenerTests {

	private final DelegatingApplicationListener listener = new DelegatingApplicationListener();

	private final StaticApplicationContext context = new StaticApplicationContext();

	@AfterEach
	public void close() {
		if (this.context != null) {
			this.context.close();
		}
	}

	@Test
	void orderedInitialize() {
		TestPropertySourceUtils.addInlinedPropertiesToEnvironment(this.context,
				"context.listener.classes=" + MockInitB.class.getName() + "," + MockInitA.class.getName());
		this.listener.onApplicationEvent(new ApplicationEnvironmentPreparedEvent(new SpringApplication(), new String[0],
				this.context.getEnvironment()));
		this.context.getBeanFactory().registerSingleton("testListener", this.listener);
		this.context.refresh();
		assertThat(this.context.getBeanFactory().getSingleton("a")).isEqualTo("a");
		assertThat(this.context.getBeanFactory().getSingleton("b")).isEqualTo("b");
	}

	@Test
	void noInitializers() {
		this.listener.onApplicationEvent(new ApplicationEnvironmentPreparedEvent(new SpringApplication(), new String[0],
				this.context.getEnvironment()));
	}

	@Test
	void emptyInitializers() {
		TestPropertySourceUtils.addInlinedPropertiesToEnvironment(this.context, "context.listener.classes:");
		this.listener.onApplicationEvent(new ApplicationEnvironmentPreparedEvent(new SpringApplication(), new String[0],
				this.context.getEnvironment()));
	}

	@Order(Ordered.HIGHEST_PRECEDENCE)
	private static class MockInitA implements ApplicationListener<ContextRefreshedEvent> {

		@Override
		public void onApplicationEvent(ContextRefreshedEvent event) {
			ConfigurableApplicationContext applicationContext = (ConfigurableApplicationContext) event
					.getApplicationContext();
			applicationContext.getBeanFactory().registerSingleton("a", "a");
		}

	}

	@Order(Ordered.LOWEST_PRECEDENCE)
	private static class MockInitB implements ApplicationListener<ContextRefreshedEvent> {

		@Override
		public void onApplicationEvent(ContextRefreshedEvent event) {
			ConfigurableApplicationContext applicationContext = (ConfigurableApplicationContext) event
					.getApplicationContext();
			assertThat(applicationContext.getBeanFactory().getSingleton("a")).isEqualTo("a");
			applicationContext.getBeanFactory().registerSingleton("b", "b");
		}

	}

}
