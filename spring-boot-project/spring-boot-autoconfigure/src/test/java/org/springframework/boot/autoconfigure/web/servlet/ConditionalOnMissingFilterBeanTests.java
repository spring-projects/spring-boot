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

package org.springframework.boot.autoconfigure.web.servlet;

import java.io.IOException;
import java.util.function.Consumer;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.junit.Test;

import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ConditionalOnMissingFilterBean}.
 *
 * @author Phillip Webb
 */
public class ConditionalOnMissingFilterBeanTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner();

	@Test
	public void outcomeWhenValueIsOfMissingBeanReturnsMatch() {

		this.contextRunner
				.withUserConfiguration(WithoutTestFilterConfig.class,
						OnMissingWithValueConfig.class)
				.run((context) -> assertThat(context)
						.satisfies(filterBeanRequirement("myOtherFilter", "testFilter")));
	}

	@Test
	public void outcomeWhenValueIsOfExistingBeanReturnsNoMatch() {
		this.contextRunner
				.withUserConfiguration(WithTestFilterConfig.class,
						OnMissingWithValueConfig.class)
				.run((context) -> assertThat(context)
						.satisfies(filterBeanRequirement("myTestFilter")));
	}

	@Test
	public void outcomeWhenValueIsOfMissingBeanRegistrationReturnsMatch() {
		this.contextRunner
				.withUserConfiguration(WithoutTestFilterRegistrationConfig.class,
						OnMissingWithValueConfig.class)
				.run((context) -> assertThat(context)
						.satisfies(filterBeanRequirement("myOtherFilter", "testFilter")));
	}

	@Test
	public void outcomeWhenValueIsOfExistingBeanRegistrationReturnsNoMatch() {
		this.contextRunner
				.withUserConfiguration(WithTestFilterRegistrationConfig.class,
						OnMissingWithValueConfig.class)
				.run((context) -> assertThat(context)
						.satisfies(filterBeanRequirement("myTestFilter")));
	}

	@Test
	public void outcomeWhenReturnTypeIsOfExistingBeanReturnsNoMatch() {
		this.contextRunner
				.withUserConfiguration(WithTestFilterConfig.class,
						OnMissingWithReturnTypeConfig.class)
				.run((context) -> assertThat(context)
						.satisfies(filterBeanRequirement("myTestFilter")));
	}

	@Test
	public void outcomeWhenReturnTypeIsOfExistingBeanRegistrationReturnsNoMatch() {
		this.contextRunner
				.withUserConfiguration(WithTestFilterRegistrationConfig.class,
						OnMissingWithReturnTypeConfig.class)
				.run((context) -> assertThat(context)
						.satisfies(filterBeanRequirement("myTestFilter")));
	}

	@Test
	public void outcomeWhenReturnRegistrationTypeIsOfExistingBeanReturnsNoMatch() {
		this.contextRunner
				.withUserConfiguration(WithTestFilterConfig.class,
						OnMissingWithReturnRegistrationTypeConfig.class)
				.run((context) -> assertThat(context)
						.satisfies(filterBeanRequirement("myTestFilter")));
	}

	@Test
	public void outcomeWhenReturnRegistrationTypeIsOfExistingBeanRegistrationReturnsNoMatch() {
		this.contextRunner
				.withUserConfiguration(WithTestFilterRegistrationConfig.class,
						OnMissingWithReturnRegistrationTypeConfig.class)
				.run((context) -> assertThat(context)
						.satisfies(filterBeanRequirement("myTestFilter")));
	}

	private Consumer<ConfigurableApplicationContext> filterBeanRequirement(
			String... names) {
		return (context) -> {
			String[] filters = context.getBeanNamesForType(Filter.class);
			String[] registrations = context
					.getBeanNamesForType(FilterRegistrationBean.class);
			assertThat(StringUtils.concatenateStringArrays(filters, registrations))
					.containsOnly(names);
		};
	}

	@Configuration
	static class WithTestFilterConfig {

		@Bean
		public TestFilter myTestFilter() {
			return new TestFilter();
		}

	}

	@Configuration
	static class WithoutTestFilterConfig {

		@Bean
		public OtherFilter myOtherFilter() {
			return new OtherFilter();
		}

	}

	@Configuration
	static class WithoutTestFilterRegistrationConfig {

		@Bean
		public FilterRegistrationBean<OtherFilter> myOtherFilter() {
			return new FilterRegistrationBean<>(new OtherFilter());
		}

	}

	@Configuration
	static class WithTestFilterRegistrationConfig {

		@Bean
		public FilterRegistrationBean<TestFilter> myTestFilter() {
			return new FilterRegistrationBean<>(new TestFilter());
		}

	}

	@Configuration
	static class OnMissingWithValueConfig {

		@Bean
		@ConditionalOnMissingFilterBean(TestFilter.class)
		public TestFilter testFilter() {
			return new TestFilter();
		}

	}

	@Configuration
	static class OnMissingWithReturnTypeConfig {

		@Bean
		@ConditionalOnMissingFilterBean
		public TestFilter testFilter() {
			return new TestFilter();
		}

	}

	@Configuration
	static class OnMissingWithReturnRegistrationTypeConfig {

		@Bean
		@ConditionalOnMissingFilterBean
		public FilterRegistrationBean<TestFilter> testFilter() {
			return new FilterRegistrationBean<>(new TestFilter());
		}

	}

	static class TestFilter implements Filter {

		@Override
		public void doFilter(ServletRequest request, ServletResponse response,
				FilterChain chain) throws IOException, ServletException {
		}

	}

	static class OtherFilter implements Filter {

		@Override
		public void doFilter(ServletRequest request, ServletResponse response,
				FilterChain chain) throws IOException, ServletException {
		}

	}

}
