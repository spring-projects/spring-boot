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

package org.springframework.boot.web.servlet;

import java.util.Collections;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterRegistration.Dynamic;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import org.springframework.boot.web.context.servlet.AnnotationConfigServletWebApplicationContext;
import org.springframework.boot.web.context.servlet.WebApplicationContextInitializer;
import org.springframework.boot.web.servlet.mock.MockFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;

/**
 * Integration tests for {@link Filter} registration.
 *
 * @author Andy Wilkinson
 */
class FilterRegistrationIntegrationTests {

	private AnnotationConfigServletWebApplicationContext context;

	@AfterEach
	void cleanUp() {
		if (this.context != null) {
			this.context.close();
		}
	}

	@Test
	void normalFiltersAreRegistered() {
		load(FilterConfiguration.class);
		then(this.context.getServletContext()).should()
			.addFilter("myFilter", this.context.getBean("myFilter", MockFilter.class));
	}

	@Test
	void scopedTargetFiltersAreNotRegistered() {
		load(ScopedTargetFilterConfiguration.class);
		then(this.context.getServletContext()).should(times(0)).addFilter(any(String.class), any(Filter.class));
	}

	private void load(Class<?> configuration) {
		ServletContext servletContext = mock(ServletContext.class);
		given(servletContext.addFilter(any(), any(Filter.class))).willReturn(mock(Dynamic.class));
		given(servletContext.getInitParameterNames()).willReturn(Collections.emptyEnumeration());
		given(servletContext.getAttributeNames()).willReturn(Collections.emptyEnumeration());
		this.context = new AnnotationConfigServletWebApplicationContext();
		this.context.setServletContext(servletContext);
		this.context.register(configuration);
		this.context.refresh();
		try {
			new WebApplicationContextInitializer(this.context).initialize(servletContext);
		}
		catch (ServletException ex) {
			throw new RuntimeException(ex);
		}
	}

	@Configuration(proxyBeanMethods = false)
	static class ScopedTargetFilterConfiguration {

		@Bean(name = "scopedTarget.myFilter")
		Filter myFilter() {
			return new MockFilter();
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class FilterConfiguration {

		@Bean
		Filter myFilter() {
			return new MockFilter();
		}

	}

}
