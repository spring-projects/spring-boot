/*
 * Copyright 2012-2017 the original author or authors.
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

package org.springframework.boot.actuate.endpoint.web.test;

import java.lang.reflect.Modifier;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.runners.BlockJUnit4ClassRunner;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.InitializationError;
import org.junit.runners.model.Statement;

import org.springframework.boot.web.servlet.context.AnnotationConfigServletWebServerApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.util.ReflectionUtils;
import org.springframework.web.util.DefaultUriBuilderFactory;
import org.springframework.web.util.DefaultUriBuilderFactory.EncodingMode;

/**
 * Base class for web endpoint runners.
 *
 * @author Andy Wilkinson
 * @author Phillip Webb
 */
abstract class AbstractWebEndpointRunner extends BlockJUnit4ClassRunner {

	private static final Duration TIMEOUT = Duration.ofMinutes(6);

	private final String name;

	private final TestContext testContext;

	protected AbstractWebEndpointRunner(Class<?> testClass, String name,
			ContextFactory contextFactory) throws InitializationError {
		super(testClass);
		this.name = name;
		this.testContext = new TestContext(testClass, contextFactory);
	}

	@Override
	protected final String getName() {
		return this.name;
	}

	@Override
	protected String testName(FrameworkMethod method) {
		return super.testName(method) + "[" + getName() + "]";
	}

	@Override
	protected Statement withBeforeClasses(Statement statement) {
		Statement delegate = super.withBeforeClasses(statement);
		return new Statement() {

			@Override
			public void evaluate() throws Throwable {
				AbstractWebEndpointRunner.this.testContext.beforeClass();
				delegate.evaluate();
			}

		};
	}

	@Override
	protected Statement withAfterClasses(Statement statement) {
		Statement delegate = super.withAfterClasses(statement);
		return new Statement() {

			@Override
			public void evaluate() throws Throwable {
				try {
					delegate.evaluate();
				}
				finally {
					AbstractWebEndpointRunner.this.testContext.afterClass();
				}
			}

		};
	}

	@Override
	protected Statement withBefores(FrameworkMethod method, Object target,
			Statement statement) {
		Statement delegate = super.withBefores(method, target, statement);
		return new Statement() {

			@Override
			public void evaluate() throws Throwable {
				AbstractWebEndpointRunner.this.testContext.beforeTest();
				delegate.evaluate();
			}

		};
	}

	@Override
	protected Statement withAfters(FrameworkMethod method, Object target,
			Statement statement) {
		Statement delegate = super.withAfters(method, target, statement);
		return new Statement() {

			@Override
			public void evaluate() throws Throwable {
				try {
					delegate.evaluate();
				}
				finally {
					AbstractWebEndpointRunner.this.testContext.afterTest();
				}
			}

		};
	}

	final class TestContext {

		private final Class<?> testClass;

		private final ContextFactory contextFactory;

		private ConfigurableApplicationContext applicationContext;

		private List<PropertySource<?>> propertySources;

		TestContext(Class<?> testClass, ContextFactory contextFactory) {
			this.testClass = testClass;
			this.contextFactory = contextFactory;
		}

		void beforeClass() {
			this.applicationContext = createApplicationContext();
			WebTestClient webTestClient = createWebTestClient();
			injectIfPossible(this.testClass, webTestClient);
			injectIfPossible(this.testClass, this.applicationContext);
		}

		void beforeTest() {
			capturePropertySources();
		}

		void afterTest() {
			restorePropertySources();
		}

		void afterClass() {
			if (this.applicationContext != null) {
				this.applicationContext.close();
			}
		}

		private ConfigurableApplicationContext createApplicationContext() {
			Class<?>[] members = this.testClass.getDeclaredClasses();
			List<Class<?>> configurationClasses = Stream.of(members)
					.filter(this::isConfiguration).collect(Collectors.toList());
			return this.contextFactory
					.createContext(new ArrayList<>(configurationClasses));
		}

		private boolean isConfiguration(Class<?> candidate) {
			return AnnotationUtils.findAnnotation(candidate, Configuration.class) != null;
		}

		private WebTestClient createWebTestClient() {
			DefaultUriBuilderFactory uriBuilderFactory = new DefaultUriBuilderFactory(
					"http://localhost:" + determinePort());
			uriBuilderFactory.setEncodingMode(EncodingMode.NONE);
			return WebTestClient.bindToServer().uriBuilderFactory(uriBuilderFactory)
					.responseTimeout(TIMEOUT).build();
		}

		private int determinePort() {
			if (this.applicationContext instanceof AnnotationConfigServletWebServerApplicationContext) {
				return ((AnnotationConfigServletWebServerApplicationContext) this.applicationContext)
						.getWebServer().getPort();
			}
			return this.applicationContext.getBean(PortHolder.class).getPort();
		}

		private void injectIfPossible(Class<?> target, Object value) {
			ReflectionUtils.doWithFields(target, (field) -> {
				if (Modifier.isStatic(field.getModifiers())
						&& field.getType().isInstance(value)) {
					ReflectionUtils.makeAccessible(field);
					ReflectionUtils.setField(field, null, value);
				}
			});
		}

		private void capturePropertySources() {
			this.propertySources = new ArrayList<>();
			this.applicationContext.getEnvironment().getPropertySources()
					.forEach(this.propertySources::add);
		}

		private void restorePropertySources() {
			List<String> names = new ArrayList<>();
			MutablePropertySources propertySources = this.applicationContext
					.getEnvironment().getPropertySources();
			propertySources
					.forEach((propertySource) -> names.add(propertySource.getName()));
			names.forEach(propertySources::remove);
			this.propertySources.forEach(propertySources::addLast);
		}

	}

}
