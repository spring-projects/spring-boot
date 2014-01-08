/*
 * Copyright 2012-2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.context.listener;

import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.SpringApplicationEnvironmentAvailableEvent;
import org.springframework.boot.test.SpringBootTestUtils;
import org.springframework.context.ApplicationListener;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.support.StaticApplicationContext;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

public class EnvironmentDelegateApplicationListenerTests {

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	private EnvironmentDelegateApplicationListener listener = new EnvironmentDelegateApplicationListener();

	private StaticApplicationContext context = new StaticApplicationContext();

	@After
	public void close() {
		if (this.context != null) {
			this.context.close();
		}
	}

	@Test
	public void orderedInitialize() throws Exception {
		SpringBootTestUtils.addEnvironment(this.context, "context.listener.classes:"
				+ MockInitB.class.getName() + "," + MockInitA.class.getName());
		this.listener.onApplicationEvent(new SpringApplicationEnvironmentAvailableEvent(
				new SpringApplication(), this.context.getEnvironment(), new String[0]));
		this.context.getBeanFactory().registerSingleton("testListener", this.listener);
		this.context.refresh();
		assertThat(this.context.getBeanFactory().getSingleton("a"), equalTo((Object) "a"));
		assertThat(this.context.getBeanFactory().getSingleton("b"), equalTo((Object) "b"));
	}

	@Test
	public void noInitializers() throws Exception {
		this.listener.onApplicationEvent(new SpringApplicationEnvironmentAvailableEvent(
				new SpringApplication(), this.context.getEnvironment(), new String[0]));
	}

	@Test
	public void emptyInitializers() throws Exception {
		SpringBootTestUtils.addEnvironment(this.context, "context.listener.classes:");
		this.listener.onApplicationEvent(new SpringApplicationEnvironmentAvailableEvent(
				new SpringApplication(), this.context.getEnvironment(), new String[0]));
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
			assertThat(applicationContext.getBeanFactory().getSingleton("a"),
					equalTo((Object) "a"));
			applicationContext.getBeanFactory().registerSingleton("b", "b");
		}
	}

}
