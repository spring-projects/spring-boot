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

package org.springframework.boot.grpc.server.autoconfigure;

import java.util.List;

import io.grpc.BindableService;
import io.grpc.CompressorRegistry;
import io.grpc.DecompressorRegistry;
import io.grpc.Grpc;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.grpc.server.GlobalServerInterceptor;
import org.springframework.grpc.server.GrpcServerFactory;
import org.springframework.grpc.server.ServerBuilderCustomizer;
import org.springframework.grpc.server.exception.CompositeGrpcExceptionHandler;
import org.springframework.grpc.server.exception.GrpcExceptionHandler;
import org.springframework.grpc.server.exception.GrpcExceptionHandlerInterceptor;
import org.springframework.grpc.server.exception.ReactiveStubBeanDefinitionRegistrar;
import org.springframework.grpc.server.service.DefaultGrpcServiceConfigurer;
import org.springframework.grpc.server.service.DefaultGrpcServiceDiscoverer;
import org.springframework.grpc.server.service.GrpcServiceConfigurer;
import org.springframework.grpc.server.service.GrpcServiceDiscoverer;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for Spring gRPC server-side
 * components.
 *
 * @author David Syer
 * @author Chris Bono
 * @author Phillip Webb
 * @since 4.1.0
 */
@AutoConfiguration
@ConditionalOnClass({ GrpcServerFactory.class, Grpc.class })
@ConditionalOnBean(BindableService.class)
@ConditionalOnBooleanProperty(name = "spring.grpc.server.enabled", matchIfMissing = true)
@EnableConfigurationProperties(GrpcServerProperties.class)
@Import({ GrpcServerCodecConfiguration.class, ServletGrpcServerConfiguration.class,
		ShadedNettyGrpcServerConfiguration.class, NettyGrpcServerConfiguration.class,
		InProcessGrpcServerConfiguration.class })
public final class GrpcServerAutoConfiguration {

	@Bean
	GrpcServerBuilderCustomizers grpcServerBuilderCustomizers(GrpcServerProperties grpcServerProperties,
			ObjectProvider<CompressorRegistry> compressorRegistry,
			ObjectProvider<DecompressorRegistry> decompressorRegistry,
			ObjectProvider<GrpcServerExecutorProvider> executorProvider,
			ObjectProvider<ServerBuilderCustomizer<?>> customizers) {
		return new GrpcServerBuilderCustomizers(grpcServerProperties, compressorRegistry, decompressorRegistry,
				executorProvider, customizers);
	}

	@Bean
	@ConditionalOnMissingBean(GrpcServiceConfigurer.class)
	DefaultGrpcServiceConfigurer grpcServiceConfigurer(ApplicationContext applicationContext) {
		return new DefaultGrpcServiceConfigurer(applicationContext);
	}

	@Bean
	@ConditionalOnMissingBean(GrpcServiceDiscoverer.class)
	DefaultGrpcServiceDiscoverer grpcServiceDiscoverer(ApplicationContext applicationContext) {
		return new DefaultGrpcServiceDiscoverer(applicationContext);
	}

	@Bean
	@GlobalServerInterceptor
	@ConditionalOnMissingBean
	GrpcExceptionHandlerInterceptor globalExceptionHandlerInterceptor(List<GrpcExceptionHandler> exceptionHandlers) {
		CompositeGrpcExceptionHandler compositeHandler = new CompositeGrpcExceptionHandler(
				exceptionHandlers.toArray(GrpcExceptionHandler[]::new));
		return new GrpcExceptionHandlerInterceptor(compositeHandler);
	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnClass(name = "com.salesforce.reactivegrpc.common.Function")
	@Import(ReactiveStubBeanDefinitionRegistrar.class)
	static class ReactiveStubConfiguration {

	}

}
