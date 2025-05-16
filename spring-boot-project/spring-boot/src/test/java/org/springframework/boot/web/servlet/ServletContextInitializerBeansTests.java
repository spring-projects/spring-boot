/*
 * Copyright 2012-2025 the original author or authors.
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

package org.springframework.boot.web.servlet;

import java.util.Collection;
import java.util.EnumSet;
import java.util.Map;

import jakarta.servlet.DispatcherType;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebInitParam;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSessionIdListener;
import org.assertj.core.api.ThrowingConsumer;
import org.junit.jupiter.api.Test;

import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.web.filter.OncePerRequestFilter;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ServletContextInitializerBeans}.
 *
 * @author Andy Wilkinson
 * @author Moritz Halbritter
 * @author Daeho Kwon
 * @author Dmytro Danilenkov
 */
class ServletContextInitializerBeansTests {

	private ConfigurableApplicationContext context;

	@Test
	void servletThatImplementsServletContextInitializerIsOnlyRegisteredOnce() {
		load(ServletConfiguration.class);
		ServletContextInitializerBeans initializerBeans = new ServletContextInitializerBeans(
				this.context.getBeanFactory());
		assertThat(initializerBeans).hasSize(1);
		assertThat(initializerBeans.iterator()).toIterable().hasOnlyElementsOfType(TestServlet.class);
	}

	@Test
	void filterThatImplementsServletContextInitializerIsOnlyRegisteredOnce() {
		load(FilterConfiguration.class);
		ServletContextInitializerBeans initializerBeans = new ServletContextInitializerBeans(
				this.context.getBeanFactory());
		assertThat(initializerBeans).hasSize(1);
		assertThat(initializerBeans.iterator()).toIterable().hasOnlyElementsOfType(TestFilter.class);
	}

	@Test
	void looksForInitializerBeansOfSpecifiedType() {
		load(TestConfiguration.class);
		ServletContextInitializerBeans initializerBeans = new ServletContextInitializerBeans(
				this.context.getBeanFactory(), TestServletContextInitializer.class);
		assertThat(initializerBeans).hasSize(1);
		assertThat(initializerBeans.iterator()).toIterable().hasOnlyElementsOfType(TestServletContextInitializer.class);
	}

	@Test
	void whenAnHttpSessionIdListenerBeanIsDefinedThenARegistrationBeanIsCreatedForIt() {
		load(HttpSessionIdListenerConfiguration.class);
		ServletContextInitializerBeans initializerBeans = new ServletContextInitializerBeans(
				this.context.getBeanFactory());
		assertThat(initializerBeans).hasSize(1);
		assertThat(initializerBeans).first()
			.isInstanceOf(ServletListenerRegistrationBean.class)
			.extracting((initializer) -> ((ServletListenerRegistrationBean<?>) initializer).getListener())
			.isInstanceOf(HttpSessionIdListener.class);
	}

	@Test
	void classesThatImplementMultipleInterfacesAreRegisteredForAllOfThem() {
		load(MultipleInterfacesConfiguration.class);
		ServletContextInitializerBeans initializerBeans = new ServletContextInitializerBeans(
				this.context.getBeanFactory());
		assertThat(initializerBeans).hasSize(3);
		assertThat(initializerBeans).element(0)
			.isInstanceOf(ServletRegistrationBean.class)
			.extracting((initializer) -> ((ServletRegistrationBean<?>) initializer).getServlet())
			.isInstanceOf(TestServletAndFilterAndListener.class);
		assertThat(initializerBeans).element(1)
			.isInstanceOf(FilterRegistrationBean.class)
			.extracting((initializer) -> ((FilterRegistrationBean<?>) initializer).getFilter())
			.isInstanceOf(TestServletAndFilterAndListener.class);
		assertThat(initializerBeans).element(2)
			.isInstanceOf(ServletListenerRegistrationBean.class)
			.extracting((initializer) -> ((ServletListenerRegistrationBean<?>) initializer).getListener())
			.isInstanceOf(TestServletAndFilterAndListener.class);
	}

	@Test
	@SuppressWarnings("unchecked")
	void shouldApplyServletRegistrationAnnotation() {
		load(ServletConfigurationWithAnnotation.class);
		ServletContextInitializerBeans initializerBeans = new ServletContextInitializerBeans(
				this.context.getBeanFactory(), TestServletContextInitializer.class);
		assertThatSingleRegistration(initializerBeans, ServletRegistrationBean.class, (servletRegistrationBean) -> {
			assertThat(servletRegistrationBean.isEnabled()).isFalse();
			assertThat(servletRegistrationBean.getOrder()).isEqualTo(Ordered.LOWEST_PRECEDENCE);
			assertThat(servletRegistrationBean.getServletName()).isEqualTo("test");
			assertThat(servletRegistrationBean.isAsyncSupported()).isFalse();
			assertThat(servletRegistrationBean.getUrlMappings()).containsExactly("/test/*");
			assertThat(servletRegistrationBean.getInitParameters())
				.containsExactlyInAnyOrderEntriesOf(Map.of("env", "test", "debug", "true"));
			assertThat(servletRegistrationBean.getMultipartConfig()).isNotNull();
			assertThat(servletRegistrationBean.getMultipartConfig().getLocation()).isEqualTo("/tmp");
			assertThat(servletRegistrationBean.getMultipartConfig().getMaxFileSize()).isEqualTo(1024);
			assertThat(servletRegistrationBean.getMultipartConfig().getMaxRequestSize()).isEqualTo(4096);
			assertThat(servletRegistrationBean.getMultipartConfig().getFileSizeThreshold()).isEqualTo(128);
		});
	}

	@Test
	@SuppressWarnings("unchecked")
	void shouldApplyFilterRegistrationAnnotation() {
		load(FilterConfigurationWithAnnotation.class);
		ServletContextInitializerBeans initializerBeans = new ServletContextInitializerBeans(
				this.context.getBeanFactory(), TestServletContextInitializer.class);
		assertThatSingleRegistration(initializerBeans, FilterRegistrationBean.class, (filterRegistrationBean) -> {
			assertThat(filterRegistrationBean.isEnabled()).isFalse();
			assertThat(filterRegistrationBean.getOrder()).isEqualTo(Ordered.LOWEST_PRECEDENCE);
			assertThat(filterRegistrationBean.getFilterName()).isEqualTo("test");
			assertThat(filterRegistrationBean.isAsyncSupported()).isFalse();
			assertThat(filterRegistrationBean.isMatchAfter()).isTrue();
			assertThat(filterRegistrationBean.getServletNames()).containsExactly("test");
			assertThat(filterRegistrationBean.determineDispatcherTypes()).containsExactly(DispatcherType.ERROR);
			assertThat(filterRegistrationBean.getUrlPatterns()).containsExactly("/test/*");
			assertThat(filterRegistrationBean.getInitParameters())
				.containsExactlyInAnyOrderEntriesOf(Map.of("env", "test", "debug", "true"));
			Collection<ServletRegistrationBean<?>> servletRegistrationBeans = filterRegistrationBean
				.getServletRegistrationBeans();
			assertThat(servletRegistrationBeans).hasSize(1);
			assertThat(servletRegistrationBeans.iterator().next().getServletName())
				.isEqualTo("testServletRegistrationBean");

		});
	}

	@Test
	@SuppressWarnings("unchecked")
	void shouldApplyFilterRegistrationAnnotationWithDefaultDispatcherTypes() {
		load(FilterConfigurationWithAnnotationAndDefaultDispatcherTypes.class);
		ServletContextInitializerBeans initializerBeans = new ServletContextInitializerBeans(
				this.context.getBeanFactory(), TestServletContextInitializer.class);
		assertThatSingleRegistration(initializerBeans, FilterRegistrationBean.class,
				(filterRegistrationBean) -> assertThat(filterRegistrationBean.determineDispatcherTypes())
					.containsExactlyElementsOf(EnumSet.allOf(DispatcherType.class)));
	}

	@Test
	void shouldApplyOrderFromBean() {
		load(OrderedServletConfiguration.class);
		ServletContextInitializerBeans initializerBeans = new ServletContextInitializerBeans(
				this.context.getBeanFactory(), TestServletContextInitializer.class);
		assertThatSingleRegistration(initializerBeans, ServletRegistrationBean.class,
				(servletRegistrationBean) -> assertThat(servletRegistrationBean.getOrder())
					.isEqualTo(OrderedTestServlet.ORDER));
	}

	@Test
	void shouldApplyOrderFromOrderAnnotationOnBeanMethod() {
		load(ServletConfigurationWithAnnotationAndOrderAnnotation.class);
		ServletContextInitializerBeans initializerBeans = new ServletContextInitializerBeans(
				this.context.getBeanFactory(), TestServletContextInitializer.class);
		assertThatSingleRegistration(initializerBeans, ServletRegistrationBean.class,
				(servletRegistrationBean) -> assertThat(servletRegistrationBean.getOrder())
					.isEqualTo(ServletConfigurationWithAnnotationAndOrderAnnotation.ORDER));
	}

	@Test
	void orderedInterfaceShouldTakePrecedenceOverOrderAnnotation() {
		load(OrderedServletConfigurationWithAnnotationAndOrder.class);
		ServletContextInitializerBeans initializerBeans = new ServletContextInitializerBeans(
				this.context.getBeanFactory(), TestServletContextInitializer.class);
		assertThatSingleRegistration(initializerBeans, ServletRegistrationBean.class,
				(servletRegistrationBean) -> assertThat(servletRegistrationBean.getOrder())
					.isEqualTo(OrderedTestServlet.ORDER));
	}

	@Test
	void shouldApplyOrderFromOrderAttribute() {
		load(ServletConfigurationWithAnnotationAndOrder.class);
		ServletContextInitializerBeans initializerBeans = new ServletContextInitializerBeans(
				this.context.getBeanFactory(), TestServletContextInitializer.class);
		assertThatSingleRegistration(initializerBeans, ServletRegistrationBean.class,
				(servletRegistrationBean) -> assertThat(servletRegistrationBean.getOrder())
					.isEqualTo(ServletConfigurationWithAnnotationAndOrder.ORDER));
	}

	private void load(Class<?>... configuration) {
		this.context = new AnnotationConfigApplicationContext(configuration);
	}

	private <T extends RegistrationBean> void assertThatSingleRegistration(
			ServletContextInitializerBeans initializerBeans, Class<T> clazz, ThrowingConsumer<T> code) {
		assertThat(initializerBeans).hasSize(1);
		ServletContextInitializer initializer = initializerBeans.iterator().next();
		assertThat(initializer).isInstanceOf(clazz);
		code.accept(clazz.cast(initializer));
	}

	@Configuration(proxyBeanMethods = false)
	static class ServletConfiguration {

		@Bean
		TestServlet testServlet() {
			return new TestServlet();
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class OrderedServletConfiguration {

		@Bean
		OrderedTestServlet testServlet() {
			return new OrderedTestServlet();
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class ServletConfigurationWithAnnotation {

		@Bean
		@ServletRegistration(enabled = false, name = "test", asyncSupported = false, urlMappings = "/test/*",
				loadOnStartup = 1,
				initParameters = { @WebInitParam(name = "env", value = "test"),
						@WebInitParam(name = "debug", value = "true") },
				multipartConfig = @MultipartConfig(location = "/tmp", maxFileSize = 1024, maxRequestSize = 4096,
						fileSizeThreshold = 128))
		TestServlet testServlet() {
			return new TestServlet();
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class ServletConfigurationWithAnnotationAndOrderAnnotation {

		static final int ORDER = 7;

		@Bean
		@ServletRegistration(name = "test")
		@Order(ORDER)
		TestServlet testServlet() {
			return new TestServlet();
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class ServletConfigurationWithAnnotationAndOrder {

		static final int ORDER = 9;

		@Bean
		@ServletRegistration(name = "test", order = ORDER)
		TestServlet testServlet() {
			return new TestServlet();
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class OrderedServletConfigurationWithAnnotationAndOrder {

		static final int ORDER = 5;

		@Bean
		@ServletRegistration
		@Order(ORDER)
		OrderedTestServlet testServlet() {
			return new OrderedTestServlet();
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class FilterConfiguration {

		@Bean
		TestFilter testFilter() {
			return new TestFilter();
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class FilterConfigurationWithAnnotation {

		@Bean
		@FilterRegistration(
				enabled = false, name = "test", asyncSupported = false, dispatcherTypes = DispatcherType.ERROR,
				matchAfter = true, servletNames = "test", urlPatterns = "/test/*", initParameters = {
						@WebInitParam(name = "env", value = "test"), @WebInitParam(name = "debug", value = "true") },
				servletClasses = { TestServlet.class })
		TestFilter testFilter() {
			return new TestFilter();
		}

		@Bean
		ServletRegistrationBean<TestServlet> testServletRegistrationBean() {
			return new ServletRegistrationBean<>(new TestServlet());
		}

		@Bean
		ServletRegistrationBean<NonMatchingServlet> nonMatchingServletRegistrationBean() {
			return new ServletRegistrationBean<>(new NonMatchingServlet());
		}

		static class NonMatchingServlet extends HttpServlet implements ServletContextInitializer {

			@Override
			public void onStartup(ServletContext servletContext) {

			}

		}

	}

	@Configuration(proxyBeanMethods = false)
	static class FilterConfigurationWithAnnotationAndDefaultDispatcherTypes {

		@Bean
		@FilterRegistration(name = "test")
		TestOncePerRequestFilter testFilter() {
			return new TestOncePerRequestFilter();
		}

		static class TestOncePerRequestFilter extends OncePerRequestFilter {

			@Override
			protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
					FilterChain filterChain) {
			}

		}

	}

	@Configuration(proxyBeanMethods = false)
	static class MultipleInterfacesConfiguration {

		@Bean
		TestServletAndFilterAndListener testServletAndFilterAndListener() {
			return new TestServletAndFilterAndListener();
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class TestConfiguration {

		@Bean
		TestServletContextInitializer testServletContextInitializer() {
			return new TestServletContextInitializer();
		}

		@Bean
		OtherTestServletContextInitializer otherTestServletContextInitializer() {
			return new OtherTestServletContextInitializer();
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class HttpSessionIdListenerConfiguration {

		@Bean
		HttpSessionIdListener httpSessionIdListener() {
			return (event, oldId) -> {
			};
		}

	}

	static class TestServlet extends HttpServlet implements ServletContextInitializer {

		@Override
		public void onStartup(ServletContext servletContext) {

		}

	}

	static class OrderedTestServlet extends HttpServlet implements ServletContextInitializer, Ordered {

		static final int ORDER = 3;

		@Override
		public void onStartup(ServletContext servletContext) {

		}

		@Override
		public int getOrder() {
			return ORDER;
		}

	}

	static class TestFilter implements Filter, ServletContextInitializer {

		@Override
		public void onStartup(ServletContext servletContext) {

		}

		@Override
		public void init(FilterConfig filterConfig) {

		}

		@Override
		public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) {

		}

	}

	static class TestServletContextInitializer implements ServletContextInitializer {

		@Override
		public void onStartup(ServletContext servletContext) {

		}

	}

	static class OtherTestServletContextInitializer implements ServletContextInitializer {

		@Override
		public void onStartup(ServletContext servletContext) {

		}

	}

}
