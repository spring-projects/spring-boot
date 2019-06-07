/*
 * Copyright 2012-2019 the original author or authors.
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

package org.springframework.boot.actuate.autoconfigure.web.servlet;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.junit.Test;

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
 */
public class CompositeHandlerExceptionResolverTests {

	private AnnotationConfigApplicationContext context;

	private MockHttpServletRequest request = new MockHttpServletRequest();

	private MockHttpServletResponse response = new MockHttpServletResponse();

	@Test
	public void resolverShouldDelegateToOtherResolversInContext() {
		load(TestConfiguration.class);
		CompositeHandlerExceptionResolver resolver = (CompositeHandlerExceptionResolver) this.context
				.getBean(DispatcherServlet.HANDLER_EXCEPTION_RESOLVER_BEAN_NAME);
		ModelAndView resolved = resolver.resolveException(this.request, this.response, null,
				new HttpRequestMethodNotSupportedException("POST"));
		assertThat(resolved.getViewName()).isEqualTo("test-view");
	}

	@Test
	public void resolverShouldAddDefaultResolverIfNonePresent() {
		load(BaseConfiguration.class);
		CompositeHandlerExceptionResolver resolver = (CompositeHandlerExceptionResolver) this.context
				.getBean(DispatcherServlet.HANDLER_EXCEPTION_RESOLVER_BEAN_NAME);
		ModelAndView resolved = resolver.resolveException(this.request, this.response, null,
				new HttpRequestMethodNotSupportedException("POST"));
		assertThat(resolved).isNotNull();
	}

	private void load(Class<?>... configs) {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
		context.register(configs);
		context.refresh();
		this.context = context;
	}

	@Configuration
	static class BaseConfiguration {

		@Bean(name = DispatcherServlet.HANDLER_EXCEPTION_RESOLVER_BEAN_NAME)
		public CompositeHandlerExceptionResolver compositeHandlerExceptionResolver() {
			return new CompositeHandlerExceptionResolver();
		}

	}

	@Configuration
	@Import(BaseConfiguration.class)
	static class TestConfiguration {

		@Bean
		public HandlerExceptionResolver testResolver() {
			return new TestHandlerExceptionResolver();
		}

	}

	static class TestHandlerExceptionResolver implements HandlerExceptionResolver {

		@Override
		public ModelAndView resolveException(HttpServletRequest request, HttpServletResponse response, Object handler,
				Exception ex) {
			return new ModelAndView("test-view");
		}

	}

}
