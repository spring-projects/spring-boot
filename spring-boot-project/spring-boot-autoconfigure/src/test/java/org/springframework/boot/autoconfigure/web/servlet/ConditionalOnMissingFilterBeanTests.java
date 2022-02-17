/*
 * Copyright 2012-2022 the original author or authors.
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

package org.springframework.boot.autoconfigure.web.servlet;

import java.io.IOException;
import java.util.function.Consumer;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import org.junit.jupiter.api.Test;

import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ConditionalOnMissingFilterBean @ConditionalOnMissingFilterBean}.
 *
 * @author Phillip Webb
 */
class ConditionalOnMissingFilterBeanTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner();

	@Test
	void outcomeWhenValueIsOfMissingBeanReturnsMatch() {

		this.contextRunner.withUserConfiguration(WithoutTestFilterConfig.class, OnMissingWithValueConfig.class)
				.run((context) -> assertThat(context).satisfies(filterBeanRequirement("myOtherFilter", "testFilter")));
	}

	@Test
	void outcomeWhenValueIsOfExistingBeanReturnsNoMatch() {
		this.contextRunner.withUserConfiguration(WithTestFilterConfig.class, OnMissingWithValueConfig.class)
				.run((context) -> assertThat(context).satisfies(filterBeanRequirement("myTestFilter")));
	}

	@Test
	void outcomeWhenValueIsOfMissingBeanRegistrationReturnsMatch() {
		this.contextRunner
				.withUserConfiguration(WithoutTestFilterRegistrationConfig.class, OnMissingWithValueConfig.class)
				.run((context) -> assertThat(context).satisfies(filterBeanRequirement("myOtherFilter", "testFilter")));
	}

	@Test
	void outcomeWhenValueIsOfExistingBeanRegistrationReturnsNoMatch() {
		this.contextRunner.withUserConfiguration(WithTestFilterRegistrationConfig.class, OnMissingWithValueConfig.class)
				.run((context) -> assertThat(context).satisfies(filterBeanRequirement("myTestFilter")));
	}

	@Test
	void outcomeWhenReturnTypeIsOfExistingBeanReturnsNoMatch() {
		this.contextRunner.withUserConfiguration(WithTestFilterConfig.class, OnMissingWithReturnTypeConfig.class)
				.run((context) -> assertThat(context).satisfies(filterBeanRequirement("myTestFilter")));
	}

	@Test
	void outcomeWhenReturnTypeIsOfExistingBeanRegistrationReturnsNoMatch() {
		this.contextRunner
				.withUserConfiguration(WithTestFilterRegistrationConfig.class, OnMissingWithReturnTypeConfig.class)
				.run((context) -> assertThat(context).satisfies(filterBeanRequirement("myTestFilter")));
	}

	@Test
	void outcomeWhenReturnRegistrationTypeIsOfExistingBeanReturnsNoMatch() {
		this.contextRunner
				.withUserConfiguration(WithTestFilterConfig.class, OnMissingWithReturnRegistrationTypeConfig.class)
				.run((context) -> assertThat(context).satisfies(filterBeanRequirement("myTestFilter")));
	}

	@Test
	void outcomeWhenReturnRegistrationTypeIsOfExistingBeanRegistrationReturnsNoMatch() {
		this.contextRunner
				.withUserConfiguration(WithTestFilterRegistrationConfig.class,
						OnMissingWithReturnRegistrationTypeConfig.class)
				.run((context) -> assertThat(context).satisfies(filterBeanRequirement("myTestFilter")));
	}

	private Consumer<ConfigurableApplicationContext> filterBeanRequirement(String... names) {
		return (context) -> {
			String[] filters = context.getBeanNamesForType(Filter.class);
			String[] registrations = context.getBeanNamesForType(FilterRegistrationBean.class);
			assertThat(StringUtils.concatenateStringArrays(filters, registrations)).containsOnly(names);
		};
	}

	@Configuration(proxyBeanMethods = false)
	static class WithTestFilterConfig {

		@Bean
		TestFilter myTestFilter() {
			return new TestFilter();
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class WithoutTestFilterConfig {

		@Bean
		OtherFilter myOtherFilter() {
			return new OtherFilter();
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class WithoutTestFilterRegistrationConfig {

		@Bean
		FilterRegistrationBean<OtherFilter> myOtherFilter() {
			return new FilterRegistrationBean<>(new OtherFilter());
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class WithTestFilterRegistrationConfig {

		@Bean
		FilterRegistrationBean<TestFilter> myTestFilter() {
			return new FilterRegistrationBean<>(new TestFilter());
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class OnMissingWithValueConfig {

		@Bean
		@ConditionalOnMissingFilterBean(TestFilter.class)
		TestFilter testFilter() {
			return new TestFilter();
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class OnMissingWithReturnTypeConfig {

		@Bean
		@ConditionalOnMissingFilterBean
		TestFilter testFilter() {
			return new TestFilter();
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class OnMissingWithReturnRegistrationTypeConfig {

		@Bean
		@ConditionalOnMissingFilterBean
		FilterRegistrationBean<TestFilter> testFilter() {
			return new FilterRegistrationBean<>(new TestFilter());
		}

	}

	static class TestFilter implements Filter {

		@Override
		public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
				throws IOException, ServletException {
		}

	}

	static class OtherFilter implements Filter {

		@Override
		public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
				throws IOException, ServletException {
		}

	}

}
