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

package org.springframework.boot.autoconfigure.security.web.servlet;

import java.util.EnumSet;

import javax.servlet.DispatcherType;

import org.junit.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.autoconfigure.security.SecurityAutoConfiguration;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.boot.web.servlet.DelegatingFilterProxyRegistrationBean;
import org.springframework.boot.web.servlet.filter.OrderedFilter;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ServletWebSecurityAutoConfiguration}.
 *
 * @author Phillip Webb
 */
public class ServletWebSecurityAutoConfigurationTests {

	private WebApplicationContextRunner contextRunner = new WebApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(SecurityAutoConfiguration.class,
					ServletWebSecurityAutoConfiguration.class,
					PropertyPlaceholderAutoConfiguration.class));

	@Test
	public void testWebConfiguration() {
		this.contextRunner.run((context) -> {
			assertThat(context.getBean(AuthenticationManagerBuilder.class)).isNotNull();
			assertThat(context.getBean(FilterChainProxy.class).getFilterChains())
					.hasSize(1);
		});
	}

	@Test
	public void testDefaultFilterOrderWithSecurityAdapter() {
		this.contextRunner
				.withConfiguration(AutoConfigurations.of(WebSecurity.class,
						SecurityFilterAutoConfiguration.class))
				.run((context) -> assertThat(context
						.getBean("securityFilterChainRegistration",
								DelegatingFilterProxyRegistrationBean.class)
						.getOrder()).isEqualTo(
								OrderedFilter.REQUEST_WRAPPER_FILTER_MAX_ORDER - 100));
	}

	@Test
	public void testDefaultFilterOrder() {
		this.contextRunner
				.withConfiguration(
						AutoConfigurations.of(SecurityFilterAutoConfiguration.class))
				.run((context) -> assertThat(context
						.getBean("securityFilterChainRegistration",
								DelegatingFilterProxyRegistrationBean.class)
						.getOrder()).isEqualTo(
								OrderedFilter.REQUEST_WRAPPER_FILTER_MAX_ORDER - 100));
	}

	@Test
	public void testCustomFilterOrder() {
		this.contextRunner
				.withConfiguration(
						AutoConfigurations.of(SecurityFilterAutoConfiguration.class))
				.withPropertyValues("spring.security.filter.order:12345").run(
						(context) -> assertThat(context
								.getBean("securityFilterChainRegistration",
										DelegatingFilterProxyRegistrationBean.class)
								.getOrder()).isEqualTo(12345));
	}

	@Test
	public void defaultFilterDispatcherTypes() {
		this.contextRunner
				.withConfiguration(
						AutoConfigurations.of(SecurityFilterAutoConfiguration.class))
				.run((context) -> {
					DelegatingFilterProxyRegistrationBean bean = context.getBean(
							"securityFilterChainRegistration",
							DelegatingFilterProxyRegistrationBean.class);
					@SuppressWarnings("unchecked")
					EnumSet<DispatcherType> dispatcherTypes = (EnumSet<DispatcherType>) ReflectionTestUtils
							.getField(bean, "dispatcherTypes");
					assertThat(dispatcherTypes).containsOnly(DispatcherType.ASYNC,
							DispatcherType.ERROR, DispatcherType.REQUEST);
				});
	}

	@Test
	public void customFilterDispatcherTypes() {
		this.contextRunner
				.withPropertyValues(
						"spring.security.filter.dispatcher-types:INCLUDE,ERROR")
				.withConfiguration(
						AutoConfigurations.of(SecurityFilterAutoConfiguration.class))
				.run((context) -> {
					DelegatingFilterProxyRegistrationBean bean = context.getBean(
							"securityFilterChainRegistration",
							DelegatingFilterProxyRegistrationBean.class);
					@SuppressWarnings("unchecked")
					EnumSet<DispatcherType> dispatcherTypes = (EnumSet<DispatcherType>) ReflectionTestUtils
							.getField(bean, "dispatcherTypes");
					assertThat(dispatcherTypes).containsOnly(DispatcherType.INCLUDE,
							DispatcherType.ERROR);
				});
	}

}
