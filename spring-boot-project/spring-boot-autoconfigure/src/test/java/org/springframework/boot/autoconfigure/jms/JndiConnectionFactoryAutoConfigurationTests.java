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
import org.junit.Test;

import org.springframework.beans.factory.BeanCreationException;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.jndi.JndiPropertiesHidingClassLoader;
import org.springframework.boot.autoconfigure.jndi.TestableInitialContextFactory;
import org.springframework.boot.test.context.assertj.AssertableApplicationContext;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.context.runner.ContextConsumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link JndiConnectionFactoryAutoConfiguration}.
 * PersistenceExceptionTranslationAutoConfigurationTests
 *
 * @author Stephane Nicoll
 */
public class JndiConnectionFactoryAutoConfigurationTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withConfiguration(
					AutoConfigurations.of(JndiConnectionFactoryAutoConfiguration.class));

	private ClassLoader threadContextClassLoader;

	private String initialContextFactory;

	@Before
	public void setupJndi() {
		this.initialContextFactory = System.getProperty(Context.INITIAL_CONTEXT_FACTORY);
		System.setProperty(Context.INITIAL_CONTEXT_FACTORY,
				TestableInitialContextFactory.class.getName());
		this.threadContextClassLoader = Thread.currentThread().getContextClassLoader();
		Thread.currentThread().setContextClassLoader(
				new JndiPropertiesHidingClassLoader(getClass().getClassLoader()));
	}

	@After
	public void cleanUp() {
		TestableInitialContextFactory.clearAll();
		if (this.initialContextFactory != null) {
			System.setProperty(Context.INITIAL_CONTEXT_FACTORY,
					this.initialContextFactory);
		}
		else {
			System.clearProperty(Context.INITIAL_CONTEXT_FACTORY);
		}
		Thread.currentThread().setContextClassLoader(this.threadContextClassLoader);
	}

	@Test
	public void detectNoAvailableCandidates() {
		this.contextRunner.run((context) -> assertThat(context)
				.doesNotHaveBean(ConnectionFactory.class));
	}

	@Test
	public void detectWithJmsXAConnectionFactory() {
		ConnectionFactory connectionFactory = configureConnectionFactory("java:/JmsXA");
		this.contextRunner.run(assertConnectionFactory(connectionFactory));
	}

	@Test
	public void detectWithXAConnectionFactory() {
		ConnectionFactory connectionFactory = configureConnectionFactory(
				"java:/XAConnectionFactory");
		this.contextRunner.run(assertConnectionFactory(connectionFactory));
	}

	@Test
	public void jndiNamePropertySet() {
		ConnectionFactory connectionFactory = configureConnectionFactory(
				"java:comp/env/myCF");
		this.contextRunner.withPropertyValues("spring.jms.jndi-name=java:comp/env/myCF")
				.run(assertConnectionFactory(connectionFactory));
	}

	@Test
	public void jndiNamePropertySetWithResourceRef() {
		ConnectionFactory connectionFactory = configureConnectionFactory(
				"java:comp/env/myCF");
		this.contextRunner.withPropertyValues("spring.jms.jndi-name=myCF")
				.run(assertConnectionFactory(connectionFactory));
	}

	@Test
	public void jndiNamePropertySetWithWrongValue() {
		this.contextRunner.withPropertyValues("spring.jms.jndi-name=doesNotExistCF")
				.run((context) -> {
					assertThat(context).hasFailed();
					assertThat(context).getFailure()
							.isInstanceOf(BeanCreationException.class)
							.hasMessageContaining("doesNotExistCF");
				});
	}

	private ContextConsumer<AssertableApplicationContext> assertConnectionFactory(
			ConnectionFactory connectionFactory) {
		return (context) -> {
			assertThat(context).hasSingleBean(ConnectionFactory.class);
			assertThat(context.getBean(ConnectionFactory.class))
					.isSameAs(connectionFactory);
		};
	}

	private ConnectionFactory configureConnectionFactory(String name) {
		ConnectionFactory connectionFactory = mock(ConnectionFactory.class);
		TestableInitialContextFactory.bind(name, connectionFactory);
		return connectionFactory;
	}

}
