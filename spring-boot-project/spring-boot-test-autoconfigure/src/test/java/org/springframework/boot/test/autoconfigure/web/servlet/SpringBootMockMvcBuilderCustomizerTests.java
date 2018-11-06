/*
 * Copyright 2012-2018 the original author or authors.
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
package org.springframework.boot.test.autoconfigure.web.servlet;

import java.util.List;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServlet;

import org.junit.Test;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.mock.web.MockServletContext;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.setup.DefaultMockMvcBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link SpringBootMockMvcBuilderCustomizer}.
 *
 * @author Madhura Bhave
 */
public class SpringBootMockMvcBuilderCustomizerTests {

	private SpringBootMockMvcBuilderCustomizer customizer;

	@Test
	@SuppressWarnings("unchecked")
	public void customizeShouldAddFilters() {
		AnnotationConfigWebApplicationContext context = new AnnotationConfigWebApplicationContext();
		MockServletContext servletContext = new MockServletContext();
		context.setServletContext(servletContext);
		context.register(ServletConfiguration.class, FilterConfiguration.class);
		context.refresh();
		DefaultMockMvcBuilder builder = MockMvcBuilders.webAppContextSetup(context);
		this.customizer = new SpringBootMockMvcBuilderCustomizer(context);
		this.customizer.customize(builder);
		FilterRegistrationBean<?> registrationBean = (FilterRegistrationBean<?>) context
				.getBean("filterRegistrationBean");
		Filter testFilter = (Filter) context.getBean("testFilter");
		Filter otherTestFilter = registrationBean.getFilter();
		List<Filter> filters = (List<Filter>) ReflectionTestUtils.getField(builder,
				"filters");
		assertThat(filters).containsExactlyInAnyOrder(testFilter, otherTestFilter);
	}

	static class ServletConfiguration {

		@Bean
		public TestServlet testServlet() {
			return new TestServlet();
		}

	}

	static class FilterConfiguration {

		@Bean
		public FilterRegistrationBean<OtherTestFilter> filterRegistrationBean() {
			return new FilterRegistrationBean<>(new OtherTestFilter());
		}

		@Bean
		public TestFilter testFilter() {
			return new TestFilter();
		}

	}

	static class TestServlet extends HttpServlet {

	}

	static class TestFilter implements Filter {

		@Override
		public void init(FilterConfig filterConfig) {

		}

		@Override
		public void doFilter(ServletRequest request, ServletResponse response,
				FilterChain chain) {

		}

		@Override
		public void destroy() {

		}

	}

	static class OtherTestFilter implements Filter {

		@Override
		public void init(FilterConfig filterConfig) {

		}

		@Override
		public void doFilter(ServletRequest request, ServletResponse response,
				FilterChain chain) {

		}

		@Override
		public void destroy() {

		}

	}

}
