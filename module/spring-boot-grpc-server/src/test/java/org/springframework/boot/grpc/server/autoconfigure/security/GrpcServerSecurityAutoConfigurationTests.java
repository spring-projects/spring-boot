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

package org.springframework.boot.grpc.server.autoconfigure.security;

import io.grpc.BindableService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.logging.ConditionEvaluationReportLoggingListener;
import org.springframework.boot.grpc.server.GrpcServletRegistration;
import org.springframework.boot.grpc.server.autoconfigure.GrpcServerExecutorProvider;
import org.springframework.boot.logging.LogLevel;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.grpc.server.GrpcServerFactory;
import org.springframework.grpc.server.exception.GrpcExceptionHandler;
import org.springframework.grpc.server.lifecycle.GrpcServerLifecycle;
import org.springframework.grpc.server.security.AuthenticationProcessInterceptor;
import org.springframework.grpc.server.security.GrpcSecurity;
import org.springframework.grpc.server.security.SecurityContextServerInterceptor;
import org.springframework.grpc.server.security.SecurityGrpcExceptionHandler;
import org.springframework.security.config.ObjectPostProcessor;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link GrpcServerSecurityAutoConfiguration}.
 *
 * @author Chris Bono
 */
class GrpcServerSecurityAutoConfigurationTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(GrpcServerSecurityAutoConfiguration.class))
		.withBean("noopServerLifecycle", GrpcServerLifecycle.class, Mockito::mock);

	@Test
	void whenSpringSecurityNotOnClasspathAutoConfigurationIsSkipped() {
		this.contextRunner.withClassLoader(new FilteredClassLoader(ObjectPostProcessor.class))
			.run((context) -> assertThat(context).doesNotHaveBean(GrpcServerSecurityAutoConfiguration.class));
	}

	@Test
	void whenGrpcNotOnClasspathAutoConfigurationIsSkipped() {
		this.contextRunner.withClassLoader(new FilteredClassLoader(BindableService.class))
			.run((context) -> assertThat(context).doesNotHaveBean(GrpcServerSecurityAutoConfiguration.class));
	}

	@Test
	void whenSpringGrpcNotOnClasspathAutoConfigurationIsSkipped() {
		this.contextRunner.withClassLoader(new FilteredClassLoader(GrpcServerFactory.class))
			.run((context) -> assertThat(context).doesNotHaveBean(GrpcServerSecurityAutoConfiguration.class));
	}

	@Test
	void whenSpringGrpcAndSpringSecurityPresentAndUsingGrpcServletCreatesGrpcSecurity() {
		new WebApplicationContextRunner()
			.withInitializer(ConditionEvaluationReportLoggingListener.forLogLevel(LogLevel.INFO))
			.withConfiguration(AutoConfigurations.of(GrpcServerSecurityAutoConfiguration.class))
			.withUserConfiguration(ServletConfiguration.class)
			.run((context) -> {
				assertThat(context).hasSingleBean(SecurityContextServerInterceptor.class);
				assertThat(context).hasSingleBean(GrpcServerExecutorProvider.class);
			});
	}

	@Test
	void whenSpringGrpcAndSpringSecurityPresentAndUsingGrpcNativeCreatesGrpcSecurity() {
		new ApplicationContextRunner()
			.withInitializer(ConditionEvaluationReportLoggingListener.forLogLevel(LogLevel.INFO))
			.withConfiguration(AutoConfigurations.of(GrpcServerSecurityAutoConfiguration.class))
			.withUserConfiguration(NativeConfiguration.class)
			.run((context) -> assertThat(context).hasSingleBean(GrpcSecurity.class));
	}

	@Test
	void whenServerEnabledPropertySetFalseThenAutoConfigurationIsSkipped() {
		this.contextRunner.withPropertyValues("spring.grpc.server.enabled=false")
			.run((context) -> assertThat(context).doesNotHaveBean(GrpcServerSecurityAutoConfiguration.class));
	}

	@Test
	void whenServerEnabledPropertyNotSetThenAutoConfigurationIsNotSkipped() {
		this.contextRunner
			.run((context) -> assertThat(context).hasSingleBean(GrpcServerSecurityAutoConfiguration.class));
	}

	@Test
	void whenServerEnabledPropertySetTrueThenAutoConfigurationIsNotSkipped() {
		this.contextRunner.withPropertyValues("spring.grpc.server.enabled=true")
			.run((context) -> assertThat(context).hasSingleBean(GrpcServerSecurityAutoConfiguration.class));
	}

	@Test
	void grpcSecurityAutoConfiguredAsExpected() {
		this.contextRunner.run((context) -> {
			assertThat(context).getBean(GrpcExceptionHandler.class).isInstanceOf(SecurityGrpcExceptionHandler.class);
			assertThat(context).getBean(AuthenticationProcessInterceptor.class).isNull();
		});
	}

	@Configuration(proxyBeanMethods = false)
	@EnableWebSecurity
	static class ServletConfiguration {

		@Bean
		GrpcServletRegistration grpcServletRegistration() {
			return new GrpcServletRegistration(mock(), mock());
		}

		@Bean
		SecurityFilterChain securityFilterChain(HttpSecurity http) {
			return http.authorizeHttpRequests((requests) -> requests.anyRequest().permitAll()).build();
		}

	}

	@Configuration(proxyBeanMethods = false)
	@EnableMethodSecurity
	static class NativeConfiguration {

		@Bean
		GrpcServerFactory grpcServerFactory() {
			return mock();
		}

	}

}
