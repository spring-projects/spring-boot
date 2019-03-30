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

package org.springframework.boot.autoconfigure.security.servlet;

import java.util.EnumSet;

import javax.servlet.DispatcherType;

import org.junit.Rule;
import org.junit.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.TestAutoConfigurationPackage;
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.test.City;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.boot.testsupport.rule.OutputCapture;
import org.springframework.boot.web.servlet.DelegatingFilterProxyRegistrationBean;
import org.springframework.boot.web.servlet.filter.OrderedFilter;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.security.authentication.AuthenticationEventPublisher;
import org.springframework.security.authentication.DefaultAuthenticationEventPublisher;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.data.repository.query.SecurityEvaluationContextExtension;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link SecurityAutoConfiguration}.
 *
 * @author Dave Syer
 * @author Rob Winch
 * @author Andy Wilkinson
 * @author Madhura Bhave
 */
public class SecurityAutoConfigurationTests {

	private WebApplicationContextRunner contextRunner = new WebApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(SecurityAutoConfiguration.class,
					PropertyPlaceholderAutoConfiguration.class));

	@Rule
	public OutputCapture output = new OutputCapture();

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
	public void testFilterIsNotRegisteredInNonWeb() {
		try (AnnotationConfigApplicationContext customContext = new AnnotationConfigApplicationContext()) {
			customContext.register(SecurityAutoConfiguration.class,
					SecurityFilterAutoConfiguration.class,
					PropertyPlaceholderAutoConfiguration.class);
			customContext.refresh();
			assertThat(customContext.containsBean("securityFilterChainRegistration"))
					.isFalse();
		}
	}

	@Test
	public void defaultAuthenticationEventPublisherRegistered() {
		this.contextRunner.run((context) -> assertThat(
				context.getBean(AuthenticationEventPublisher.class))
						.isInstanceOf(DefaultAuthenticationEventPublisher.class));
	}

	@Test
	public void defaultAuthenticationEventPublisherIsConditionalOnMissingBean() {
		this.contextRunner
				.withUserConfiguration(AuthenticationEventPublisherConfiguration.class)
				.run((context) -> assertThat(
						context.getBean(AuthenticationEventPublisher.class)).isInstanceOf(
								AuthenticationEventPublisherConfiguration.TestAuthenticationEventPublisher.class));
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
	public void testJpaCoexistsHappily() {
		this.contextRunner
				.withPropertyValues("spring.datasource.url:jdbc:hsqldb:mem:testsecdb",
						"spring.datasource.initialization-mode:never")
				.withUserConfiguration(EntityConfiguration.class)
				.withConfiguration(
						AutoConfigurations.of(HibernateJpaAutoConfiguration.class,
								DataSourceAutoConfiguration.class))
				.run((context) -> assertThat(context.getBean(JpaTransactionManager.class))
						.isNotNull());
		// This can fail if security @Conditionals force early instantiation of the
		// HibernateJpaAutoConfiguration (e.g. the EntityManagerFactory is not found)
	}

	@Test
	public void testSecurityEvaluationContextExtensionSupport() {
		this.contextRunner.run((context) -> assertThat(context)
				.getBean(SecurityEvaluationContextExtension.class).isNotNull());
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

	@Configuration(proxyBeanMethods = false)
	@TestAutoConfigurationPackage(City.class)
	protected static class EntityConfiguration {

	}

	@Configuration(proxyBeanMethods = false)
	static class AuthenticationEventPublisherConfiguration {

		@Bean
		public AuthenticationEventPublisher authenticationEventPublisher() {
			return new TestAuthenticationEventPublisher();
		}

		class TestAuthenticationEventPublisher implements AuthenticationEventPublisher {

			@Override
			public void publishAuthenticationSuccess(Authentication authentication) {

			}

			@Override
			public void publishAuthenticationFailure(AuthenticationException exception,
					Authentication authentication) {

			}

		}

	}

	@Configuration(proxyBeanMethods = false)
	@EnableWebSecurity
	static class WebSecurity extends WebSecurityConfigurerAdapter {

	}

}
