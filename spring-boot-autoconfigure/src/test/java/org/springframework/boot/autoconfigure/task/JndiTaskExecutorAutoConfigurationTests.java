/*
 * Copyright 2012-2016 the original author or authors.
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

package org.springframework.boot.autoconfigure.task;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.naming.Context;
import javax.naming.NamingException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import org.springframework.boot.autoconfigure.jndi.JndiPropertiesHidingClassLoader;
import org.springframework.boot.autoconfigure.jndi.TestableInitialContextFactory;
import org.springframework.boot.test.util.EnvironmentTestUtils;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.DefaultManagedTaskExecutor;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link JndiTaskExecutorAutoConfiguration}.
 *
 * @author Vedran Pavic
 */
public class JndiTaskExecutorAutoConfigurationTests {

	private final AnnotationConfigApplicationContext context =
			new AnnotationConfigApplicationContext();

	private String initialContextFactory;

	private ClassLoader threadContextClassLoader;

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
	public void taskExecutorIsAvailableFromJndi()
			throws IllegalStateException, NamingException {
		ExecutorService executorService = Executors.newSingleThreadExecutor();
		configureJndi("foo", executorService);

		EnvironmentTestUtils.addEnvironment(this.context, "spring.task.jndi-name=foo");
		registerAndRefresh(JndiTaskExecutorAutoConfiguration.class);

		assertThat(this.context.getBean(TaskExecutor.class))
				.isInstanceOf(DefaultManagedTaskExecutor.class);
	}

	private void configureJndi(String name, ExecutorService executorService)
			throws IllegalStateException, NamingException {
		TestableInitialContextFactory.bind(name, executorService);
	}

	private void registerAndRefresh(Class<?>... annotatedClasses) {
		this.context.register(annotatedClasses);
		this.context.refresh();
	}

}
