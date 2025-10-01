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
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.logging.ConditionEvaluationReportLoggingListener;
import org.springframework.boot.autoconfigure.ssl.SslAutoConfiguration;
import org.springframework.boot.context.annotation.UserConfigurations;
import org.springframework.boot.context.event.ApplicationFailedEvent;
import org.springframework.boot.grpc.server.autoconfigure.GrpcServerAutoConfiguration;
import org.springframework.boot.grpc.server.autoconfigure.GrpcServerFactoryAutoConfiguration;
import org.springframework.boot.logging.LogLevel;
import org.springframework.boot.security.autoconfigure.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.boot.web.context.servlet.AnnotationConfigServletWebApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.grpc.server.GlobalServerInterceptor;
import org.springframework.grpc.server.lifecycle.GrpcServerLifecycle;
import org.springframework.grpc.server.security.AuthenticationProcessInterceptor;
import org.springframework.grpc.server.security.GrpcSecurity;
import org.springframework.security.config.Customizer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link GrpcServerAutoConfiguration}.
 *
 * @author Chris Bono
 */
class OAuth2ResourceServerAutoConfigurationTests {

	private BindableService service = mock();

	{
		ServerServiceDefinition serviceDefinition = ServerServiceDefinition.builder("my-service").build();
		given(this.service.bindService()).willReturn(serviceDefinition);

	}

	private ApplicationContextRunner contextRunner() {
		// NOTE: we use noop server lifecycle to avoid startup
		return new ApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(OAuth2ResourceServerAutoConfiguration.class,
					GrpcSecurityAutoConfiguration.class))
			.withBean(BindableService.class, () -> this.service)
			.withBean("noopServerLifecycle", GrpcServerLifecycle.class, Mockito::mock);
	}

	@Disabled("TODO fix GrpcSecurity mismatch w/ Spring Security 6/7")
	@Test
	void jwtConfiguredWhenIssuerIsProvided() {
		this.contextRunner()
			.withPropertyValues("spring.security.oauth2.resourceserver.jwt.issuer-uri=http://localhost:9000")
			.run((context) -> assertThat(context).hasSingleBean(AuthenticationProcessInterceptor.class));
	}

	@Disabled("TODO fix GrpcSecurity mismatch w/ Spring Security 6/7")
	@Test
	void jwtConfiguredWhenJwkSetIsProvided() {
		this.contextRunner()
			.withPropertyValues("spring.security.oauth2.resourceserver.jwt.jwk-set-uri=http://localhost:9000")
			.run((context) -> assertThat(context).hasSingleBean(AuthenticationProcessInterceptor.class));
	}

	@Disabled("TODO fix GrpcSecurity mismatch w/ Spring Security 6/7")
	@Test
	void customInterceptorWhenJwkSetIsProvided() {
		this.contextRunner()
			.withInitializer(ConditionEvaluationReportLoggingListener.forLogLevel(LogLevel.INFO))
			.withConfiguration(UserConfigurations.of(CustomInterceptorConfiguration.class))
			.withPropertyValues("spring.security.oauth2.resourceserver.jwt.jwk-set-uri=http://localhost:9000")
			.run((context) -> assertThat(context).hasSingleBean(AuthenticationProcessInterceptor.class));
	}

	@Test
	void notConfiguredWhenIssuerNotProvided() {
		this.contextRunner()
			.run((context) -> assertThat(context).doesNotHaveBean(AuthenticationProcessInterceptor.class));
	}

	@Test
	void notConfiguredInWebApplication() {
		new WebApplicationContextRunner().withConfiguration(AutoConfigurations.of(
				GrpcServerFactoryAutoConfiguration.class, GrpcServerAutoConfiguration.class,
				SecurityAutoConfiguration.class,
				org.springframework.boot.security.oauth2.server.resource.autoconfigure.servlet.OAuth2ResourceServerAutoConfiguration.class,
				OAuth2ResourceServerAutoConfiguration.class, GrpcSecurityAutoConfiguration.class))
			.withBean(BindableService.class, () -> this.service)
			.withPropertyValues("spring.security.oauth2.resourceserver.jwt.issuer-uri=http://localhost:9000")
			.run((context) -> assertThat(context).doesNotHaveBean(AuthenticationProcessInterceptor.class));
	}

	@Test
	void notConfiguredInWebApplicationWithNoBindableService() {
		new WebApplicationContextRunner(WebApplicationContextRunner.withMockServletContext(MyContext::new))
			.withInitializer(ConditionEvaluationReportLoggingListener.forLogLevel(LogLevel.INFO))
			.withConfiguration(AutoConfigurations.of(GrpcServerFactoryAutoConfiguration.class,
					GrpcServerAutoConfiguration.class, SecurityAutoConfiguration.class,
					org.springframework.boot.security.oauth2.server.resource.autoconfigure.servlet.OAuth2ResourceServerAutoConfiguration.class,
					OAuth2ResourceServerAutoConfiguration.class, GrpcSecurityAutoConfiguration.class))
			.withPropertyValues("spring.security.oauth2.resourceserver.jwt.issuer-uri=http://localhost:9000")
			.run((context) -> assertThat(context).doesNotHaveBean(AuthenticationProcessInterceptor.class));
	}

	// @Disabled("TODO fix GrpcSecurity mismatch w/ Spring Security 6/7")
	@Test
	void configuredInWebApplicationWithGrpcNative() {
		new WebApplicationContextRunner(WebApplicationContextRunner.withMockServletContext(MyContext::new))
			.withConfiguration(AutoConfigurations.of(GrpcServerFactoryAutoConfiguration.class,
					GrpcServerAutoConfiguration.class, SslAutoConfiguration.class, SecurityAutoConfiguration.class,
					org.springframework.boot.security.oauth2.server.resource.autoconfigure.servlet.OAuth2ResourceServerAutoConfiguration.class,
					OAuth2ResourceServerAutoConfiguration.class, GrpcSecurityAutoConfiguration.class))
			.withInitializer(ConditionEvaluationReportLoggingListener.forLogLevel(LogLevel.INFO))
			.withBean(BindableService.class, () -> this.service)
			.withBean("noopServerLifecycle", GrpcServerLifecycle.class, Mockito::mock)
			.withPropertyValues("spring.security.oauth2.resourceserver.jwt.issuer-uri=http://localhost:9000",
					"spring.grpc.server.servlet.enabled=false", "spring.grpc.server.port=0")
			.run((context) -> assertThat(context).hasSingleBean(AuthenticationProcessInterceptor.class));
	}

	// Utility class to ensure ApplicationFailedEvent is published
	static class MyContext extends AnnotationConfigServletWebApplicationContext {

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

}
