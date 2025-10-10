/*
 * Copyright 2012-present the original author or authors.
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

package org.springframework.boot.web.servlet.support;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Map;
import java.util.Vector;
import java.util.concurrent.atomic.AtomicBoolean;

import jakarta.servlet.DispatcherType;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterRegistration.Dynamic;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.ServletException;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent;
import org.springframework.boot.context.logging.LoggingApplicationListener;
import org.springframework.boot.testsupport.system.CapturedOutput;
import org.springframework.boot.testsupport.system.OutputCaptureExtension;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.ApplicationListener;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.core.Ordered;
import org.springframework.core.env.PropertySource;
import org.springframework.mock.web.MockServletContext;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.StandardServletEnvironment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link SpringBootServletInitializer}.
 *
 * @author Phillip Webb
 * @author Andy Wilkinson
 */
@ExtendWith(OutputCaptureExtension.class)
class SpringBootServletInitializerTests {

	private final ServletContext servletContext = new MockServletContext();

	private @Nullable SpringApplication application;

	@AfterEach
	void verifyLoggingOutput(CapturedOutput output) {
		assertThat(output).doesNotContain(StandardServletEnvironment.class.getSimpleName());
	}

	@Test
	void failsWithoutConfigure() {
		assertThatIllegalStateException()
			.isThrownBy(() -> new MockSpringBootServletInitializer().createRootApplicationContext(this.servletContext))
			.withMessageContaining("No SpringApplication sources have been defined");
	}

	@Test
	void withConfigurationAnnotation() {
		new WithConfigurationAnnotation().createRootApplicationContext(this.servletContext);
		assertThat(this.application).isNotNull();
		assertThat(this.application.getAllSources()).containsOnly(WithConfigurationAnnotation.class,
				ErrorPageFilterConfiguration.class);
	}

	@Test
	void withConfiguredSource() {
		new WithConfiguredSource().createRootApplicationContext(this.servletContext);
		assertThat(this.application).isNotNull();
		assertThat(this.application.getAllSources()).containsOnly(Config.class, ErrorPageFilterConfiguration.class);
	}

	@Test
	void applicationBuilderCanBeCustomized() {
		CustomSpringBootServletInitializer servletInitializer = new CustomSpringBootServletInitializer();
		servletInitializer.createRootApplicationContext(this.servletContext);
		assertThat(servletInitializer.applicationBuilder.built).isTrue();
	}

	@Test
	void mainClassHasSensibleDefault() {
		new WithConfigurationAnnotation().createRootApplicationContext(this.servletContext);
		assertThat(this.application).hasFieldOrPropertyWithValue("mainApplicationClass",
				WithConfigurationAnnotation.class);
	}

	@Test
	void shutdownHooksAreNotRegistered() throws ServletException {
		new WithConfigurationAnnotation().onStartup(this.servletContext);
		assertThat(this.servletContext.getAttribute(LoggingApplicationListener.REGISTER_SHUTDOWN_HOOK_PROPERTY))
			.isEqualTo(false);
		assertThat(this.application).isNotNull();
		Object properties = ReflectionTestUtils.getField(this.application, "properties");
		assertThat(properties).hasFieldOrPropertyWithValue("registerShutdownHook", false);
	}

	@Test
	void errorPageFilterRegistrationCanBeDisabled() {
		try (AbstractApplicationContext context = (AbstractApplicationContext) new WithErrorPageFilterNotRegistered()
			.createRootApplicationContext(this.servletContext)) {
			assertThat(context).isNotNull();
			Map<String, ErrorPageFilter> errorPageFilterBeans = context.getBeansOfType(ErrorPageFilter.class);
			assertThat(errorPageFilterBeans).isEmpty();
		}
	}

	@Test
	@SuppressWarnings("rawtypes")
	void errorPageFilterIsRegisteredWithNearHighestPrecedence() {
		ServletContext servletContext = mock(ServletContext.class);
		given(servletContext.addFilter(any(), any(Filter.class))).willReturn(mock(Dynamic.class));
		given(servletContext.getInitParameterNames()).willReturn(Collections.emptyEnumeration());
		given(servletContext.getAttributeNames()).willReturn(Collections.emptyEnumeration());
		try (AbstractApplicationContext context = (AbstractApplicationContext) new WithErrorPageFilter()
			.createRootApplicationContext(servletContext)) {
			assertThat(context).isNotNull();
			Map<String, FilterRegistrationBean> registrations = context.getBeansOfType(FilterRegistrationBean.class);
			assertThat(registrations).hasSize(1);
			FilterRegistrationBean errorPageFilterRegistration = registrations.get("errorPageFilterRegistration");
			assertThat(errorPageFilterRegistration).isNotNull();
			assertThat(errorPageFilterRegistration.getOrder()).isEqualTo(Ordered.HIGHEST_PRECEDENCE + 1);
		}
	}

	@Test
	@SuppressWarnings("rawtypes")
	void errorPageFilterIsRegisteredForRequestAndAsyncDispatch() {
		ServletContext servletContext = mock(ServletContext.class);
		given(servletContext.addFilter(any(), any(Filter.class))).willReturn(mock(Dynamic.class));
		given(servletContext.getInitParameterNames()).willReturn(Collections.emptyEnumeration());
		given(servletContext.getAttributeNames()).willReturn(Collections.emptyEnumeration());
		try (AbstractApplicationContext context = (AbstractApplicationContext) new WithErrorPageFilter()
			.createRootApplicationContext(servletContext)) {
			assertThat(context).isNotNull();
			Map<String, FilterRegistrationBean> registrations = context.getBeansOfType(FilterRegistrationBean.class);
			assertThat(registrations).hasSize(1);
			FilterRegistrationBean errorPageFilterRegistration = registrations.get("errorPageFilterRegistration");
			assertThat(errorPageFilterRegistration).hasFieldOrPropertyWithValue("dispatcherTypes",
					EnumSet.of(DispatcherType.ASYNC, DispatcherType.REQUEST));
		}
	}

	@Test
	void executableWarThatUsesServletInitializerDoesNotHaveErrorPageFilterConfigured() {
		try (ConfigurableApplicationContext context = new SpringApplication(ExecutableWar.class).run()) {
			assertThat(context.getBeansOfType(ErrorPageFilter.class)).isEmpty();
		}
	}

	@Test
	void servletContextPropertySourceIsAvailablePriorToRefresh() {
		ServletContext servletContext = mock(ServletContext.class);
		given(servletContext.addFilter(any(), any(Filter.class))).willReturn(mock(Dynamic.class));
		given(servletContext.getInitParameterNames())
			.willReturn(Collections.enumeration(Collections.singletonList("spring.profiles.active")));
		given(servletContext.getInitParameter("spring.profiles.active")).willReturn("from-servlet-context");
		given(servletContext.getAttributeNames()).willReturn(Collections.emptyEnumeration());
		try (ConfigurableApplicationContext context = (ConfigurableApplicationContext) new PropertySourceVerifyingSpringBootServletInitializer()
			.createRootApplicationContext(servletContext)) {
			assertThat(context).isNotNull();
			assertThat(context.getEnvironment().getActiveProfiles()).containsExactly("from-servlet-context");
		}
	}

	@Test
	void whenServletContextIsDestroyedThenJdbcDriversAreDeregistered() throws ServletException {
		ServletContext servletContext = mock(ServletContext.class);
		given(servletContext.addFilter(any(), any(Filter.class))).willReturn(mock(Dynamic.class));
		given(servletContext.getInitParameterNames()).willReturn(new Vector<String>().elements());
		given(servletContext.getAttributeNames()).willReturn(new Vector<String>().elements());
		AtomicBoolean driversDeregistered = new AtomicBoolean();
		new SpringBootServletInitializer() {

			@Override
			protected SpringApplicationBuilder configure(SpringApplicationBuilder builder) {
				return builder.sources(Config.class);
			}

			@Override
			protected void deregisterJdbcDrivers(ServletContext servletContext) {
				driversDeregistered.set(true);
			}

		}.onStartup(servletContext);
		ArgumentCaptor<ServletContextListener> captor = ArgumentCaptor.forClass(ServletContextListener.class);
		then(servletContext).should().addListener(captor.capture());
		captor.getValue().contextDestroyed(new ServletContextEvent(servletContext));
		assertThat(driversDeregistered).isTrue();
	}

	@Test
	void whenServletContextIsDestroyedThenReactorSchedulersAreShutDown() throws ServletException {
		ServletContext servletContext = mock(ServletContext.class);
		given(servletContext.addFilter(any(), any(Filter.class))).willReturn(mock(Dynamic.class));
		given(servletContext.getInitParameterNames()).willReturn(new Vector<String>().elements());
		given(servletContext.getAttributeNames()).willReturn(new Vector<String>().elements());
		AtomicBoolean schedulersShutDown = new AtomicBoolean();
		new SpringBootServletInitializer() {

			@Override
			protected SpringApplicationBuilder configure(SpringApplicationBuilder builder) {
				return builder.sources(Config.class);
			}

			@Override
			protected void shutDownSharedReactorSchedulers(ServletContext servletContext) {
				schedulersShutDown.set(true);
			}

		}.onStartup(servletContext);
		ArgumentCaptor<ServletContextListener> captor = ArgumentCaptor.forClass(ServletContextListener.class);
		then(servletContext).should().addListener(captor.capture());
		captor.getValue().contextDestroyed(new ServletContextEvent(servletContext));
		assertThat(schedulersShutDown).isTrue();
	}

	static class PropertySourceVerifyingSpringBootServletInitializer extends SpringBootServletInitializer {

		@Override
		protected SpringApplicationBuilder configure(SpringApplicationBuilder builder) {
			return builder.sources(TestApp.class).listeners(new PropertySourceVerifyingApplicationListener());
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class TestApp {

	}

	private class MockSpringBootServletInitializer extends SpringBootServletInitializer {

		@Override
		protected @Nullable WebApplicationContext run(SpringApplication application) {
			SpringBootServletInitializerTests.this.application = application;
			return null;
		}

	}

	private final class CustomSpringBootServletInitializer extends MockSpringBootServletInitializer {

		private final CustomSpringApplicationBuilder applicationBuilder = new CustomSpringApplicationBuilder();

		@Override
		protected SpringApplicationBuilder createSpringApplicationBuilder() {
			return this.applicationBuilder;
		}

		@Override
		protected SpringApplicationBuilder configure(SpringApplicationBuilder application) {
			return application.sources(Config.class);
		}

	}

	@Configuration(proxyBeanMethods = false)
	public class WithConfigurationAnnotation extends MockSpringBootServletInitializer {

	}

	public class WithConfiguredSource extends MockSpringBootServletInitializer {

		@Override
		protected SpringApplicationBuilder configure(SpringApplicationBuilder application) {
			return application.sources(Config.class);
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class WithErrorPageFilterNotRegistered extends SpringBootServletInitializer {

		WithErrorPageFilterNotRegistered() {
			setRegisterErrorPageFilter(false);
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class WithErrorPageFilter extends SpringBootServletInitializer {

	}

	@Configuration(proxyBeanMethods = false)
	static class ExecutableWar extends SpringBootServletInitializer {

	}

	@Configuration(proxyBeanMethods = false)
	static class Config {

	}

	static class CustomSpringApplicationBuilder extends SpringApplicationBuilder {

		private boolean built;

		@Override
		public SpringApplication build() {
			this.built = true;
			return super.build();
		}

	}

	private static final class PropertySourceVerifyingApplicationListener
			implements ApplicationListener<ApplicationEnvironmentPreparedEvent> {

		@Override
		public void onApplicationEvent(ApplicationEnvironmentPreparedEvent event) {
			PropertySource<?> propertySource = event.getEnvironment()
				.getPropertySources()
				.get(StandardServletEnvironment.SERVLET_CONTEXT_PROPERTY_SOURCE_NAME);
			assertThat(propertySource).isNotNull();
			assertThat(propertySource.getProperty("spring.profiles.active")).isEqualTo("from-servlet-context");
		}

	}

}
