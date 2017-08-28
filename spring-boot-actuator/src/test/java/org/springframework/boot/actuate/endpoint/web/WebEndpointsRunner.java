/*
 * Copyright 2012-2017 the original author or authors.
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

package org.springframework.boot.actuate.endpoint.web;

import java.lang.reflect.Modifier;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.glassfish.jersey.server.ResourceConfig;
import org.junit.runner.Runner;
import org.junit.runners.BlockJUnit4ClassRunner;
import org.junit.runners.Suite;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.InitializationError;
import org.junit.runners.model.Statement;

import org.springframework.boot.actuate.autoconfigure.ManagementContextAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.endpoint.infrastructure.EndpointInfrastructureAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.endpoint.infrastructure.ServletEndpointAutoConfiguration;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.http.HttpMessageConvertersAutoConfiguration;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.autoconfigure.jersey.JerseyAutoConfiguration;
import org.springframework.boot.autoconfigure.web.reactive.WebFluxAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.DispatcherServletAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.WebMvcAutoConfiguration;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.boot.web.context.WebServerInitializedEvent;
import org.springframework.boot.web.embedded.netty.NettyReactiveWebServerFactory;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.reactive.context.ReactiveWebServerApplicationContext;
import org.springframework.boot.web.servlet.context.AnnotationConfigServletWebServerApplicationContext;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertySource;
import org.springframework.http.server.reactive.HttpHandler;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.util.ReflectionUtils;
import org.springframework.web.server.adapter.WebHttpHandlerBuilder;
import org.springframework.web.util.DefaultUriBuilderFactory;
import org.springframework.web.util.DefaultUriBuilderFactory.EncodingMode;

/**
 * A custom {@link Runner} that tests web endpoints that are made available over HTTP
 * using Jersey, Spring MVC, and WebFlux.
 * <p>
 * The following types can be automatically injected into static fields on the test class:
 * <ul>
 * <li>{@link WebTestClient}</li>
 * <li>{@link ConfigurableApplicationContext}</li>
 * </ul>
 * <p>
 * The {@link PropertySource PropertySources} that belong to the application context's
 * {@link org.springframework.core.env.Environment} are reset at the end of every test.
 * This means that {@link TestPropertyValues} can be used in a test without affecting the
 * {@code Environment} of other tests in the same class. The runner always sets the flag
 * `endpoints.default.web.enabled` to true so that web endpoints are enabled.
 *
 * @author Andy Wilkinson
 */
public class WebEndpointsRunner extends Suite {

	public WebEndpointsRunner(Class<?> testClass) throws InitializationError {
		super(testClass, createRunners(testClass));
	}

	private static List<Runner> createRunners(Class<?> clazz) throws InitializationError {
		return Arrays.asList(new JerseyWebEndpointsRunner(clazz),
				new MvcWebEndpointsRunner(clazz), new ReactiveWebEndpointsRunner(clazz));
	}

	private static class AbstractWebEndpointsRunner extends BlockJUnit4ClassRunner {

		private final TestContext testContext;

		private final String name;

		protected AbstractWebEndpointsRunner(Class<?> klass, String name,
				Function<List<Class<?>>, ConfigurableApplicationContext> contextLoader)
						throws InitializationError {
			super(klass);
			this.name = name;
			this.testContext = new TestContext(klass, contextLoader);
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
					AbstractWebEndpointsRunner.this.testContext.beforeClass();
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
						AbstractWebEndpointsRunner.this.testContext.afterClass();
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
					AbstractWebEndpointsRunner.this.testContext.beforeTest();
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
						AbstractWebEndpointsRunner.this.testContext.afterTest();
					}
				}

			};
		}

	}

	private static final class MvcWebEndpointsRunner extends AbstractWebEndpointsRunner {

		private MvcWebEndpointsRunner(Class<?> klass) throws InitializationError {
			super(klass, "Spring MVC", (classes) -> {
				AnnotationConfigServletWebServerApplicationContext context = new AnnotationConfigServletWebServerApplicationContext();
				TestPropertyValues.of("endpoints.default.web.enabled=true")
						.applyTo(context);
				classes.add(MvcTestConfiguration.class);
				context.register(classes.toArray(new Class<?>[classes.size()]));
				context.refresh();
				return context;
			});
		}

		@Configuration
		@ImportAutoConfiguration({ JacksonAutoConfiguration.class,
				HttpMessageConvertersAutoConfiguration.class,
				WebMvcAutoConfiguration.class, DispatcherServletAutoConfiguration.class,
				EndpointInfrastructureAutoConfiguration.class,
				ManagementContextAutoConfiguration.class,
				ServletEndpointAutoConfiguration.class })
		static class MvcTestConfiguration {

			@Bean
			public TomcatServletWebServerFactory tomcat() {
				return new TomcatServletWebServerFactory(0);
			}

		}

	}

	private static final class JerseyWebEndpointsRunner
			extends AbstractWebEndpointsRunner {

		private JerseyWebEndpointsRunner(Class<?> klass) throws InitializationError {
			super(klass, "Jersey", (classes) -> {
				AnnotationConfigServletWebServerApplicationContext context = new AnnotationConfigServletWebServerApplicationContext();
				TestPropertyValues.of("endpoints.default.web.enabled=true")
						.applyTo(context);
				classes.add(JerseyAppConfiguration.class);
				classes.add(JerseyInfrastructureConfiguration.class);
				context.register(classes.toArray(new Class<?>[classes.size()]));
				context.refresh();
				return context;
			});
		}

		@Configuration
		@Import({ JacksonAutoConfiguration.class, JerseyAutoConfiguration.class,
				EndpointInfrastructureAutoConfiguration.class,
				ManagementContextAutoConfiguration.class })
		static class JerseyInfrastructureConfiguration {

			@Bean
			public TomcatServletWebServerFactory tomcat() {
				return new TomcatServletWebServerFactory(0);
			}

		}

		@Configuration
		static class JerseyAppConfiguration {

			@Bean
			public ResourceConfig resourceConfig() {
				return new ResourceConfig();
			}

		}

	}

	private static final class ReactiveWebEndpointsRunner
			extends AbstractWebEndpointsRunner {

		private ReactiveWebEndpointsRunner(Class<?> klass) throws InitializationError {
			super(klass, "Reactive", (classes) -> {
				ReactiveWebServerApplicationContext context = new ReactiveWebServerApplicationContext();
				TestPropertyValues.of("endpoints.default.web.enabled:true")
						.applyTo(context);
				classes.add(ReactiveInfrastructureConfiguration.class);
				context.register(classes.toArray(new Class<?>[classes.size()]));
				context.refresh();
				return context;
			});
		}

		@Configuration
		@ImportAutoConfiguration({ JacksonAutoConfiguration.class,
				WebFluxAutoConfiguration.class,
				EndpointInfrastructureAutoConfiguration.class,
				ManagementContextAutoConfiguration.class })
		static class ReactiveInfrastructureConfiguration
				implements ApplicationListener<WebServerInitializedEvent> {

			@Bean
			public NettyReactiveWebServerFactory netty() {
				return new NettyReactiveWebServerFactory(0);
			}

			@Override
			public void onApplicationEvent(WebServerInitializedEvent event) {
				portHolder().setPort(event.getWebServer().getPort());
			}

			@Bean
			public HttpHandler httpHandler(ApplicationContext applicationContext) {
				return WebHttpHandlerBuilder.applicationContext(applicationContext)
						.build();
			}

			@Bean
			public PortHolder portHolder() {
				return new PortHolder();
			}

		}

	}

	private static final class PortHolder {

		private int port;

		int getPort() {
			return this.port;
		}

		void setPort(int port) {
			this.port = port;
		}

	}

	private static final class TestContext {

		private final Class<?> testClass;

		private final Function<List<Class<?>>, ConfigurableApplicationContext> applicationContextLoader;

		private ConfigurableApplicationContext applicationContext;

		private List<PropertySource<?>> propertySources;

		TestContext(Class<?> testClass,
				Function<List<Class<?>>, ConfigurableApplicationContext> applicationContextLoader) {
			this.testClass = testClass;
			this.applicationContextLoader = applicationContextLoader;
		}

		private void beforeClass() {
			this.applicationContext = createApplicationContext();
			WebTestClient webTestClient = createWebTestClient();
			injectIfPossible(this.testClass, webTestClient);
			injectIfPossible(this.testClass, this.applicationContext);
		}

		private void beforeTest() {
			capturePropertySources();
		}

		private void afterTest() {
			restorePropertySources();
		}

		private void afterClass() {
			if (this.applicationContext != null) {
				this.applicationContext.close();
			}
		}

		private ConfigurableApplicationContext createApplicationContext() {
			return this.applicationContextLoader
					.apply(new ArrayList<>(Stream.of(this.testClass.getDeclaredClasses())
							.filter((candidate) -> AnnotationUtils.findAnnotation(
									candidate, Configuration.class) != null)
					.collect(Collectors.toList())));
		}

		private WebTestClient createWebTestClient() {
			DefaultUriBuilderFactory uriBuilderFactory = new DefaultUriBuilderFactory(
					"http://localhost:" + determinePort());
			uriBuilderFactory.setEncodingMode(EncodingMode.NONE);
			WebTestClient webTestClient = WebTestClient.bindToServer()
					.uriBuilderFactory(uriBuilderFactory)
					.responseTimeout(Duration.ofSeconds(30)).build();
			return webTestClient;
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
