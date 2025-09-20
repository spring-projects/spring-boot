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

import io.grpc.internal.GrpcUtil;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.grpc.server.autoconfigure.ConditionalOnGrpcNativeServer;
import org.springframework.boot.grpc.server.autoconfigure.ConditionalOnGrpcServerEnabled;
import org.springframework.boot.grpc.server.autoconfigure.ConditionalOnGrpcServletServer;
import org.springframework.boot.grpc.server.autoconfigure.GrpcServerExecutorProvider;
import org.springframework.boot.grpc.server.autoconfigure.exception.GrpcExceptionHandlerAutoConfiguration;
import org.springframework.boot.grpc.server.autoconfigure.security.GrpcSecurityAutoConfiguration.ExceptionHandlerConfiguration;
import org.springframework.boot.grpc.server.autoconfigure.security.GrpcSecurityAutoConfiguration.GrpcNativeSecurityConfigurerConfiguration;
import org.springframework.boot.grpc.server.autoconfigure.security.GrpcSecurityAutoConfiguration.GrpcServletSecurityConfigurerConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.grpc.server.GlobalServerInterceptor;
import org.springframework.grpc.server.GrpcServerFactory;
import org.springframework.grpc.server.exception.GrpcExceptionHandler;
import org.springframework.grpc.server.security.GrpcSecurity;
import org.springframework.grpc.server.security.SecurityContextServerInterceptor;
import org.springframework.grpc.server.security.SecurityGrpcExceptionHandler;
import org.springframework.security.concurrent.DelegatingSecurityContextExecutor;
import org.springframework.security.config.ObjectPostProcessor;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.web.SecurityFilterChain;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for gRPC server-side security.
 *
 * @author Dave Syer
 * @author Chris Bono
 * @author Andrey Litvitski
 * @since 4.0.0
 */
@AutoConfiguration(before = GrpcExceptionHandlerAutoConfiguration.class,
		afterName = "org.springframework.boot.security.autoconfigure.servlet.SecurityAutoConfiguration")
@ConditionalOnGrpcServerEnabled
@ConditionalOnClass({ GrpcServerFactory.class, ObjectPostProcessor.class })
@Import({ ExceptionHandlerConfiguration.class, GrpcNativeSecurityConfigurerConfiguration.class,
		GrpcServletSecurityConfigurerConfiguration.class })
public final class GrpcSecurityAutoConfiguration {

	@Configuration(proxyBeanMethods = false)
	@Import(AuthenticationConfiguration.class)
	static class ExceptionHandlerConfiguration {

		@Bean
		GrpcExceptionHandler accessExceptionHandler() {
			return new SecurityGrpcExceptionHandler();
		}

	}

	@ConditionalOnBean(ObjectPostProcessor.class)
	@Configuration(proxyBeanMethods = false)
	@ConditionalOnGrpcNativeServer
	static class GrpcNativeSecurityConfigurerConfiguration {

		@Bean
		GrpcSecurity grpcSecurity(ObjectPostProcessor<Object> objectPostProcessor,
				AuthenticationConfiguration authenticationConfiguration, ApplicationContext context) throws Exception {
			AuthenticationManagerBuilder authenticationManagerBuilder = authenticationConfiguration
				.authenticationManagerBuilder(objectPostProcessor, context);
			authenticationManagerBuilder
				.parentAuthenticationManager(authenticationConfiguration.getAuthenticationManager());
			return new GrpcSecurity(objectPostProcessor, authenticationManagerBuilder, context);
		}

	}

	@ConditionalOnBean(SecurityFilterChain.class)
	@ConditionalOnGrpcServletServer
	@Configuration(proxyBeanMethods = false)
	static class GrpcServletSecurityConfigurerConfiguration {

		@Bean
		@GlobalServerInterceptor
		SecurityContextServerInterceptor securityContextInterceptor() {
			return new SecurityContextServerInterceptor();
		}

		@Bean
		@ConditionalOnMissingBean(GrpcServerExecutorProvider.class)
		GrpcServerExecutorProvider grpcServerExecutorProvider() {
			return () -> new DelegatingSecurityContextExecutor(GrpcUtil.SHARED_CHANNEL_EXECUTOR.create());
		}

	}

}
