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
import io.grpc.ServerServiceDefinition;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.logging.ConditionEvaluationReportLoggingListener;
import org.springframework.boot.context.annotation.UserConfigurations;
import org.springframework.boot.context.event.ApplicationFailedEvent;
import org.springframework.boot.logging.LogLevel;
import org.springframework.boot.security.autoconfigure.web.servlet.ServletWebSecurityAutoConfiguration;
import org.springframework.boot.security.oauth2.server.resource.autoconfigure.OAuth2ResourceServerAutoConfiguration;
import org.springframework.boot.test.context.assertj.ApplicationContextAssertProvider;
import org.springframework.boot.test.context.runner.AbstractApplicationContextRunner;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.boot.web.context.servlet.AnnotationConfigServletWebApplicationContext;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.grpc.server.GlobalServerInterceptor;
import org.springframework.grpc.server.lifecycle.GrpcServerLifecycle;
import org.springframework.grpc.server.security.AuthenticationProcessInterceptor;
import org.springframework.grpc.server.security.GrpcSecurity;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.ObjectPostProcessor;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link GrpcServerOAuth2ResourceServerAutoConfiguration}.
 *
 * @author Chris Bono
 */
class GrpcServerOAuth2ResourceServerAutoConfigurationTests {

	private static final AutoConfigurations autoConfigurations = AutoConfigurations
		.of(OAuth2ResourceServerAutoConfiguration.class, GrpcServerOAuth2ResourceServerAutoConfiguration.class);

	private ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withConfiguration(autoConfigurations)
		.withUserConfiguration(GrpcSecurityConfiguration.class)
		.with(this::serviceBean)
		.withBean("noopServerLifecycle", GrpcServerLifecycle.class, Mockito::mock);

	@Test
	void jwtConfiguredWhenIssuerIsProvided() {
		this.contextRunner
			.withPropertyValues("spring.security.oauth2.resourceserver.jwt.issuer-uri=http://localhost:9000")
			.run((context) -> assertThat(context).hasSingleBean(AuthenticationProcessInterceptor.class));
	}

	@Test
	void jwtConfiguredWhenJwkSetIsProvided() {
		this.contextRunner
			.withPropertyValues("spring.security.oauth2.resourceserver.jwt.jwk-set-uri=http://localhost:9000")
			.run((context) -> assertThat(context).hasSingleBean(AuthenticationProcessInterceptor.class));
	}

	@Test
	void customInterceptorWhenJwkSetIsProvided() {
		this.contextRunner.withInitializer(ConditionEvaluationReportLoggingListener.forLogLevel(LogLevel.INFO))
			.withConfiguration(UserConfigurations.of(CustomInterceptorConfiguration.class))
			.withPropertyValues("spring.security.oauth2.resourceserver.jwt.jwk-set-uri=http://localhost:9000")
			.run((context) -> assertThat(context).hasSingleBean(AuthenticationProcessInterceptor.class));
	}

	@Test
	void notConfiguredWhenIssuerNotProvided() {
		this.contextRunner
			.run((context) -> assertThat(context).doesNotHaveBean(AuthenticationProcessInterceptor.class));
	}

	@Test
	void notConfiguredInWebApplication() {
		new WebApplicationContextRunner().withConfiguration(autoConfigurations)
			.withConfiguration(AutoConfigurations.of(ServletWebSecurityAutoConfiguration.class,
					OAuth2ResourceServerAutoConfiguration.class))
			.with(this::serviceBean)
			.withPropertyValues("spring.security.oauth2.resourceserver.jwt.issuer-uri=http://localhost:9000")
			.run((context) -> assertThat(context).doesNotHaveBean(AuthenticationProcessInterceptor.class));
	}

	@Test
	void notConfiguredInWebApplicationWithNoBindableService() {
		new WebApplicationContextRunner().withConfiguration(autoConfigurations)
			.withConfiguration(AutoConfigurations.of(ServletWebSecurityAutoConfiguration.class,
					OAuth2ResourceServerAutoConfiguration.class))
			.withPropertyValues("spring.security.oauth2.resourceserver.jwt.issuer-uri=http://localhost:9000")
			.run((context) -> assertThat(context).doesNotHaveBean(AuthenticationProcessInterceptor.class));
	}

	private <R extends AbstractApplicationContextRunner<R, C, A>, C extends ConfigurableApplicationContext, A extends ApplicationContextAssertProvider<C>> R serviceBean(
			R contextRunner) {
		BindableService service = mock();
		ServerServiceDefinition serviceDefinition = ServerServiceDefinition.builder("my-service").build();
		given(service.bindService()).willReturn(serviceDefinition);
		return contextRunner.withBean(BindableService.class, () -> service);
	}

	static class FailingApplicationFailedEventContext extends AnnotationConfigServletWebApplicationContext {

		@Override
		public void refresh() {
			try {
				super.refresh();
			}
			catch (Throwable ex) {
				publishEvent(new ApplicationFailedEvent(new SpringApplication(this), new String[0], this, ex));
				throw ex;
			}
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class CustomInterceptorConfiguration {

		@Bean
		@GlobalServerInterceptor
		AuthenticationProcessInterceptor jwtSecurityFilterChain(GrpcSecurity grpc) throws Exception {
			return grpc.authorizeRequests((requests) -> requests.allRequests().authenticated())
				.oauth2ResourceServer((resourceServer) -> resourceServer.jwt(Customizer.withDefaults()))
				.build();
		}

	}

	@Configuration(proxyBeanMethods = false)
	@EnableWebSecurity
	static class GrpcSecurityConfiguration {

		@Bean
		GrpcSecurity grpcSecurity(ApplicationContext context, ObjectPostProcessor<Object> objectPostProcessor,
				AuthenticationConfiguration authenticationConfiguration) {
			AuthenticationManagerBuilder authenticationManagerBuilder = authenticationConfiguration
				.authenticationManagerBuilder(objectPostProcessor, context);
			authenticationManagerBuilder
				.parentAuthenticationManager(authenticationConfiguration.getAuthenticationManager());
			return new GrpcSecurity(objectPostProcessor, authenticationManagerBuilder, context);
		}

	}

}
