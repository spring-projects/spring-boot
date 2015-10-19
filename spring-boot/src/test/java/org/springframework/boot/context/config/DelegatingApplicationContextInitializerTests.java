/*
 * Copyright 2012-2014 the original author or authors.
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

package org.springframework.boot.context.config;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import org.springframework.boot.test.EnvironmentTestUtils;
import org.springframework.context.ApplicationContextException;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.StaticApplicationContext;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.web.context.ConfigurableWebApplicationContext;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

/**
 * Tests for {@link DelegatingApplicationContextInitializer}.
 *
 * @author Phillip Webb
 */
public class DelegatingApplicationContextInitializerTests {

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	private final DelegatingApplicationContextInitializer initializer = new DelegatingApplicationContextInitializer();

	@Test
	public void orderedInitialize() throws Exception {
		StaticApplicationContext context = new StaticApplicationContext();
		EnvironmentTestUtils.addEnvironment(context, "context.initializer.classes:"
				+ MockInitB.class.getName() + "," + MockInitA.class.getName());
		this.initializer.initialize(context);
		assertThat(context.getBeanFactory().getSingleton("a"), equalTo((Object) "a"));
		assertThat(context.getBeanFactory().getSingleton("b"), equalTo((Object) "b"));
	}

	@Test
	public void noInitializers() throws Exception {
		StaticApplicationContext context = new StaticApplicationContext();
		this.initializer.initialize(context);
	}

	@Test
	public void emptyInitializers() throws Exception {
		StaticApplicationContext context = new StaticApplicationContext();
		EnvironmentTestUtils.addEnvironment(context, "context.initializer.classes:");
		this.initializer.initialize(context);
	}

	@Test
	public void noSuchInitializerClass() throws Exception {
		StaticApplicationContext context = new StaticApplicationContext();
		EnvironmentTestUtils.addEnvironment(context,
				"context.initializer.classes:missing.madeup.class");
		this.thrown.expect(ApplicationContextException.class);
		this.initializer.initialize(context);
	}

	@Test
	public void notAnInitializerClass() throws Exception {
		StaticApplicationContext context = new StaticApplicationContext();
		EnvironmentTestUtils.addEnvironment(context,
				"context.initializer.classes:" + Object.class.getName());
		this.thrown.expect(IllegalArgumentException.class);
		this.initializer.initialize(context);
	}

	@Test
	public void genericNotSuitable() throws Exception {
		StaticApplicationContext context = new StaticApplicationContext();
		EnvironmentTestUtils.addEnvironment(context,
				"context.initializer.classes:" + NotSuitableInit.class.getName());
		this.thrown.expect(IllegalArgumentException.class);
		this.thrown.expectMessage("generic parameter");
		this.initializer.initialize(context);
	}

	@Order(Ordered.HIGHEST_PRECEDENCE)
	private static class MockInitA
			implements ApplicationContextInitializer<ConfigurableApplicationContext> {
		@Override
		public void initialize(ConfigurableApplicationContext applicationContext) {
			applicationContext.getBeanFactory().registerSingleton("a", "a");
		}
	}

	@Order(Ordered.LOWEST_PRECEDENCE)
	private static class MockInitB
			implements ApplicationContextInitializer<ConfigurableApplicationContext> {
		@Override
		public void initialize(ConfigurableApplicationContext applicationContext) {
			assertThat(applicationContext.getBeanFactory().getSingleton("a"),
					equalTo((Object) "a"));
			applicationContext.getBeanFactory().registerSingleton("b", "b");
		}
	}

	private static class NotSuitableInit
			implements ApplicationContextInitializer<ConfigurableWebApplicationContext> {
		@Override
		public void initialize(ConfigurableWebApplicationContext applicationContext) {
		}
	}
}
