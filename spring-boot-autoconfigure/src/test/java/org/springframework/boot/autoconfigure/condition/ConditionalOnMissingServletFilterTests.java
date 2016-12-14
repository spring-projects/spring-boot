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

package org.springframework.boot.autoconfigure.condition;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ConditionalOnMissingServletFilter}.
 *
 * @author Brian Clozel
 */
public class ConditionalOnMissingServletFilterTests {

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	private final AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();

	@Test
	public void testMissingFilter() {
		this.context.register(TestSingleFilterConfiguration.class);
		this.context.refresh();
		assertThat(this.context.getBeansOfType(CustomServletFilter.class).isEmpty()).isTrue();
		assertThat(this.context.getBean("foo")).isEqualTo("foo");
	}

	@Test
	public void testExistingFilter() {
		this.context.register(CustomServletFilterConfiguration.class, TestSingleFilterConfiguration.class);
		this.context.refresh();
		assertThat(this.context.getBeansOfType(CustomServletFilter.class))
				.containsKey("customServletFilter").hasSize(1);
		assertThat(this.context.containsBean("foo")).isFalse();
	}

	@Test
	public void testExistingFilterRegistration() {
		this.context.register(CustomFilterRegistrationConfiguration.class, TestSingleFilterConfiguration.class);
		this.context.refresh();
		assertThat(this.context.getBeansOfType(FilterRegistrationBean.class))
				.containsKey("customServletFilterRegistration").hasSize(1);
		assertThat(this.context.containsBean("foo")).isFalse();
	}

	@Test
	public void testMultipleFiltersCondition() {
		this.context.register(OtherServletFilterConfiguration.class, CustomFilterRegistrationConfiguration.class,
				TestMultipleFiltersConfiguration.class);
		this.context.refresh();
		assertThat(this.context.getBeansOfType(OtherServletFilter.class))
				.containsKey("otherServletFilter").hasSize(1);
		assertThat(this.context.getBeansOfType(FilterRegistrationBean.class))
				.containsKey("customServletFilterRegistration").hasSize(1);
		assertThat(this.context.containsBean("foo")).isFalse();
	}

	@Configuration
	protected static class CustomServletFilterConfiguration {

		@Bean
		public CustomServletFilter customServletFilter() {
			return new CustomServletFilter();
		}

	}

	@Configuration
	protected static class OtherServletFilterConfiguration {

		@Bean
		public OtherServletFilter otherServletFilter() {
			return new OtherServletFilter();
		}

	}

	@Configuration
	protected static class CustomFilterRegistrationConfiguration {

		@Bean
		public FilterRegistrationBean customServletFilterRegistration() {
			FilterRegistrationBean filter = new FilterRegistrationBean();
			filter.setFilter(new CustomServletFilter());
			return filter;
		}

	}

	@Configuration
	protected static class TestSingleFilterConfiguration {

		@Bean
		@ConditionalOnMissingServletFilter(CustomServletFilter.class)
		public String foo() {
			return "foo";
		}

	}

	@Configuration
	protected static class TestMultipleFiltersConfiguration {

		@Bean
		@ConditionalOnMissingServletFilter({CustomServletFilter.class, OtherServletFilter.class})
		public String foo() {
			return "foo";
		}

	}

	static class CustomServletFilter implements Filter {

		@Override
		public void init(FilterConfig filterConfig) throws ServletException {
		}

		@Override
		public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain)
				throws IOException, ServletException {
		}

		@Override
		public void destroy() {
		}
	}

	static class OtherServletFilter implements Filter {

		@Override
		public void init(FilterConfig filterConfig) throws ServletException {
		}

		@Override
		public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain)
				throws IOException, ServletException {
		}

		@Override
		public void destroy() {
		}
	}
}
