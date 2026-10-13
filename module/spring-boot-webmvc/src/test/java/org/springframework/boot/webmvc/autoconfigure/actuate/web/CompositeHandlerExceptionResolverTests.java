/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.webmvc.autoconfigure.actuate.web;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.servlet.DispatcherServlet;
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.web.servlet.ModelAndView;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link CompositeHandlerExceptionResolver}.
 *
 * @author Madhura Bhave
 * @author Scott Frederick
 */
class CompositeHandlerExceptionResolverTests {

	private @Nullable AnnotationConfigApplicationContext context;

	private final MockHttpServletRequest request = new MockHttpServletRequest();

	private final MockHttpServletResponse response = new MockHttpServletResponse();

	@Test
	void resolverShouldDelegateToOtherResolversInContext() {
		load(TestConfiguration.class);
		assertThat(this.context).isNotNull();
		CompositeHandlerExceptionResolver resolver = (CompositeHandlerExceptionResolver) this.context
			.getBean(DispatcherServlet.HANDLER_EXCEPTION_RESOLVER_BEAN_NAME);
		ModelAndView resolved = resolver.resolveException(this.request, this.response, null,
				new HttpRequestMethodNotSupportedException("POST"));
		assertThat(resolved).isNotNull();
		assertThat(resolved.getViewName()).isEqualTo("test-view");
	}

	@Test
	void resolverShouldAddDefaultResolverIfNonePresent() {
		load(BaseConfiguration.class);
		assertThat(this.context).isNotNull();
		CompositeHandlerExceptionResolver resolver = (CompositeHandlerExceptionResolver) this.context
			.getBean(DispatcherServlet.HANDLER_EXCEPTION_RESOLVER_BEAN_NAME);
		HttpRequestMethodNotSupportedException exception = new HttpRequestMethodNotSupportedException("POST");
		ModelAndView resolved = resolver.resolveException(this.request, this.response, null, exception);
		assertThat(resolved).isNotNull();
		assertThat(resolved.isEmpty()).isTrue();
	}

	private void load(Class<?>... configs) {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
		context.register(configs);
		context.refresh();
		this.context = context;
	}

	@Configuration(proxyBeanMethods = false)
	static class BaseConfiguration {

		@Bean(name = DispatcherServlet.HANDLER_EXCEPTION_RESOLVER_BEAN_NAME)
		CompositeHandlerExceptionResolver compositeHandlerExceptionResolver() {
			return new CompositeHandlerExceptionResolver();
		}

	}

	@Configuration(proxyBeanMethods = false)
	@Import(BaseConfiguration.class)
	static class TestConfiguration {

		@Bean
		HandlerExceptionResolver testResolver() {
			return new TestHandlerExceptionResolver();
		}

	}

	static class TestHandlerExceptionResolver implements HandlerExceptionResolver {

		@Override
		public ModelAndView resolveException(HttpServletRequest request, HttpServletResponse response,
				@Nullable Object handler, Exception ex) {
			return new ModelAndView("test-view");
		}

	}

}
