/*
 * Copyright 2012-2018 the original author or authors.
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

package org.springframework.boot.autoconfigure.jms;

import javax.jms.ConnectionFactory;
import javax.naming.Context;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import org.springframework.beans.factory.BeanCreationException;
import org.springframework.boot.autoconfigure.jndi.JndiPropertiesHidingClassLoader;
import org.springframework.boot.autoconfigure.jndi.TestableInitialContextFactory;
import org.springframework.boot.test.util.EnvironmentTestUtils;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link JndiConnectionFactoryAutoConfiguration}.
 *
 * @author Stephane Nicoll
 */
public class JndiConnectionFactoryAutoConfigurationTests {

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	private ClassLoader threadContextClassLoader;

	private String initialContextFactory;

	private AnnotationConfigApplicationContext context;

	@Before
	public void setupJndi() {
		this.initialContextFactory = System.getProperty(Context.INITIAL_CONTEXT_FACTORY);
		System.setProperty(Context.INITIAL_CONTEXT_FACTORY,
				TestableInitialContextFactory.class.getName());
	}

	@Before
	public void setupThreadContextClassLoader() {
		this.threadContextClassLoader = Thread.currentThread().getContextClassLoader();
		Thread.currentThread().setContextClassLoader(
				new JndiPropertiesHidingClassLoader(getClass().getClassLoader()));
	}

	@After
	public void close() {
		TestableInitialContextFactory.clearAll();
		if (this.initialContextFactory != null) {
			System.setProperty(Context.INITIAL_CONTEXT_FACTORY,
					this.initialContextFactory);
		}
		else {
			System.clearProperty(Context.INITIAL_CONTEXT_FACTORY);
		}
		if (this.context != null) {
			this.context.close();
		}
		Thread.currentThread().setContextClassLoader(this.threadContextClassLoader);
	}

	@Test
	public void detectNoAvailableCandidates() {
		load();
		assertThat(this.context.getBeansOfType(ConnectionFactory.class)).isEmpty();
	}

	@Test
	public void detectWithJmsXAConnectionFactory() {
		ConnectionFactory connectionFactory = configureConnectionFactory("java:/JmsXA");
		load();
		assertConnectionFactory(connectionFactory);
	}

	@Test
	public void detectWithXAConnectionFactory() {
		ConnectionFactory connectionFactory = configureConnectionFactory(
				"java:/XAConnectionFactory");
		load();
		assertConnectionFactory(connectionFactory);
	}

	@Test
	public void jndiNamePropertySet() {
		ConnectionFactory connectionFactory = configureConnectionFactory("myCF");
		load("spring.jms.jndi-name=myCF");
		assertConnectionFactory(connectionFactory);
	}

	@Test
	public void jndiNamePropertySetWithWrongValue() {
		this.thrown.expect(BeanCreationException.class);
		this.thrown.expectMessage("doesNotExistCF");
		load("spring.jms.jndi-name=doesNotExistCF");
	}

	private void assertConnectionFactory(ConnectionFactory connectionFactory) {
		assertThat(this.context.getBeansOfType(ConnectionFactory.class)).hasSize(1);
		assertThat(this.context.getBean(ConnectionFactory.class))
				.isSameAs(connectionFactory);
	}

	private void load(String... environment) {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
		EnvironmentTestUtils.addEnvironment(ctx, environment);
		ctx.register(JndiConnectionFactoryAutoConfiguration.class);
		ctx.refresh();
		this.context = ctx;
	}

	private ConnectionFactory configureConnectionFactory(String name) {
		ConnectionFactory connectionFactory = mock(ConnectionFactory.class);
		TestableInitialContextFactory.bind(name, connectionFactory);
		return connectionFactory;
	}

}
