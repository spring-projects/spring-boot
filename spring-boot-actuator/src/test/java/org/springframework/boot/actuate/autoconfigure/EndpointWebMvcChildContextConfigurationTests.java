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
package org.springframework.boot.actuate.autoconfigure;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockServletContext;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.web.servlet.ModelAndView;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link EndpointWebMvcChildContextConfiguration}.
 *
 * @author Madhura Bhave
 */
public class EndpointWebMvcChildContextConfigurationTests {

	private AnnotationConfigWebApplicationContext context;

	private MockHttpServletRequest request = new MockHttpServletRequest();

	private MockHttpServletResponse response = new MockHttpServletResponse();

	@Before
	public void setup() {
		this.context = new AnnotationConfigWebApplicationContext();
		this.context.setServletContext(new MockServletContext());
	}

	@After
	public void close() {
		if (this.context != null) {
			this.context.close();
		}
	}

	@Test
	public void compositeResolverShouldAddDefaultResolverIfNonePresent() {
		this.context.register(BaseConfiguration.class);
		this.context.setParent(getParentApplicationContext());
		this.context.refresh();
		EndpointWebMvcChildContextConfiguration.CompositeHandlerExceptionResolver resolver = this.context
				.getBean(
						EndpointWebMvcChildContextConfiguration.CompositeHandlerExceptionResolver.class);
		ModelAndView resolved = resolver.resolveException(this.request, this.response,
				null, new HttpRequestMethodNotSupportedException("POST"));
		assertThat(resolved).isNotNull();
	}

	@Test
	public void compositeResolverShouldDelegateToOtherResolversInContext() {
		this.context.register(HandlerExceptionResolverConfiguration.class);
		this.context.setParent(getParentApplicationContext());
		this.context.refresh();
		EndpointWebMvcChildContextConfiguration.CompositeHandlerExceptionResolver resolver = this.context
				.getBean(
						EndpointWebMvcChildContextConfiguration.CompositeHandlerExceptionResolver.class);
		ModelAndView resolved = resolver.resolveException(this.request, this.response,
				null, new HttpRequestMethodNotSupportedException("POST"));
		assertThat(resolved.getViewName()).isEqualTo("test-view");
	}

	private AnnotationConfigWebApplicationContext getParentApplicationContext() {
		AnnotationConfigWebApplicationContext parent = new AnnotationConfigWebApplicationContext();
		parent.refresh();
		return parent;
	}

	@Configuration
	@Import(EndpointWebMvcChildContextConfiguration.class)
	@EnableConfigurationProperties(ManagementServerProperties.class)
	static class BaseConfiguration {

	}

	@Configuration
	@Import(BaseConfiguration.class)
	static class HandlerExceptionResolverConfiguration {

		@Bean
		public HandlerExceptionResolver testResolver() {
			return new TestHandlerExceptionResolver();
		}

	}

	static class TestHandlerExceptionResolver implements HandlerExceptionResolver {

		@Override
		public ModelAndView resolveException(HttpServletRequest request,
				HttpServletResponse response, Object handler, Exception ex) {
			return new ModelAndView("test-view");
		}

	}

}
