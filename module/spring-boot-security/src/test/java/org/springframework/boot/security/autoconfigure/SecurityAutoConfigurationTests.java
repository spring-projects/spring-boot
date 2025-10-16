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

package org.springframework.boot.security.autoconfigure;

import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.TestAutoConfigurationPackage;
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration;
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration;
import org.springframework.boot.security.autoconfigure.jpa.City;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.security.authentication.AuthenticationEventPublisher;
import org.springframework.security.authentication.DefaultAuthenticationEventPublisher;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.data.repository.query.SecurityEvaluationContextExtension;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link SecurityAutoConfiguration}.
 *
 * @author Dave Syer
 * @author Rob Winch
 * @author Andy Wilkinson
 * @author Madhura Bhave
 */
class SecurityAutoConfigurationTests {

	private final WebApplicationContextRunner contextRunner = new WebApplicationContextRunner().withConfiguration(
			AutoConfigurations.of(SecurityAutoConfiguration.class, PropertyPlaceholderAutoConfiguration.class));

	@Test
	void defaultAuthenticationEventPublisherRegistered() {
		this.contextRunner.run((context) -> assertThat(context.getBean(AuthenticationEventPublisher.class))
			.isInstanceOf(DefaultAuthenticationEventPublisher.class));
	}

	@Test
	void defaultAuthenticationEventPublisherIsConditionalOnMissingBean() {
		this.contextRunner.withUserConfiguration(AuthenticationEventPublisherConfiguration.class)
			.run((context) -> assertThat(context.getBean(AuthenticationEventPublisher.class))
				.isInstanceOf(AuthenticationEventPublisherConfiguration.TestAuthenticationEventPublisher.class));
	}

	@Test
	void testJpaCoexistsHappily() {
		this.contextRunner.withPropertyValues("spring.datasource.url:jdbc:hsqldb:mem:testsecdb")
			.withUserConfiguration(EntityConfiguration.class)
			.withConfiguration(
					AutoConfigurations.of(HibernateJpaAutoConfiguration.class, DataSourceAutoConfiguration.class))
			.run((context) -> assertThat(context.getBean(JpaTransactionManager.class)).isNotNull());
		// This can fail if security @Conditionals force early instantiation of the
		// HibernateJpaAutoConfiguration (e.g. the EntityManagerFactory is not found)
	}

	@Test
	void testSecurityEvaluationContextExtensionSupport() {
		this.contextRunner
			.run((context) -> assertThat(context).getBean(SecurityEvaluationContextExtension.class).isNotNull());
	}

	@Configuration(proxyBeanMethods = false)
	@TestAutoConfigurationPackage(City.class)
	static class EntityConfiguration {

	}

	@Configuration(proxyBeanMethods = false)
	static class AuthenticationEventPublisherConfiguration {

		@Bean
		AuthenticationEventPublisher authenticationEventPublisher() {
			return new TestAuthenticationEventPublisher();
		}

		class TestAuthenticationEventPublisher implements AuthenticationEventPublisher {

			@Override
			public void publishAuthenticationSuccess(Authentication authentication) {

			}

			@Override
			public void publishAuthenticationFailure(AuthenticationException exception, Authentication authentication) {

			}

		}

	}

}
