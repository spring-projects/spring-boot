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

import io.grpc.BindableService;
import io.grpc.CompressorRegistry;
import io.grpc.DecompressorRegistry;
import io.grpc.ServerBuilder;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.grpc.server.autoconfigure.codec.GrpcCodecConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.Ordered;
import org.springframework.grpc.server.GrpcServerFactory;
import org.springframework.grpc.server.ServerBuilderCustomizer;
import org.springframework.grpc.server.exception.ReactiveStubBeanDefinitionRegistrar;
import org.springframework.grpc.server.service.DefaultGrpcServiceConfigurer;
import org.springframework.grpc.server.service.DefaultGrpcServiceDiscoverer;
import org.springframework.grpc.server.service.GrpcServiceConfigurer;
import org.springframework.grpc.server.service.GrpcServiceDiscoverer;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for Spring gRPC server-side
 * components.
 * <p>
 * Spring gRPC must be on the classpath and at least one {@link BindableService} bean
 * registered in the context in order for the auto-configuration to execute.
 *
 * @author David Syer
 * @author Chris Bono
 * @since 4.0.0
 */
@AutoConfiguration(after = GrpcServerFactoryAutoConfiguration.class)
@ConditionalOnGrpcServerEnabled
@ConditionalOnClass({ GrpcServerFactory.class })
@ConditionalOnBean(BindableService.class)
@EnableConfigurationProperties(GrpcServerProperties.class)
@Import({ GrpcCodecConfiguration.class })
public final class GrpcServerAutoConfiguration {

	@ConditionalOnMissingBean
	@Bean
	ServerBuilderCustomizers serverBuilderCustomizers(ObjectProvider<ServerBuilderCustomizer<?>> customizers) {
		return new ServerBuilderCustomizers(customizers.orderedStream().toList());
	}

	@ConditionalOnMissingBean(GrpcServiceConfigurer.class)
	@Bean
	DefaultGrpcServiceConfigurer grpcServiceConfigurer(ApplicationContext applicationContext) {
		return new DefaultGrpcServiceConfigurer(applicationContext);
	}

	@ConditionalOnMissingBean(GrpcServiceDiscoverer.class)
	@Bean
	DefaultGrpcServiceDiscoverer grpcServiceDiscoverer(ApplicationContext applicationContext) {
		return new DefaultGrpcServiceDiscoverer(applicationContext);
	}

	@ConditionalOnBean(CompressorRegistry.class)
	@Bean
	<T extends ServerBuilder<T>> ServerBuilderCustomizer<T> compressionServerConfigurer(CompressorRegistry registry) {
		return (builder) -> builder.compressorRegistry(registry);
	}

	@ConditionalOnBean(DecompressorRegistry.class)
	@Bean
	<T extends ServerBuilder<T>> ServerBuilderCustomizer<T> decompressionServerConfigurer(
			DecompressorRegistry registry) {
		return (builder) -> builder.decompressorRegistry(registry);
	}

	@ConditionalOnBean(GrpcServerExecutorProvider.class)
	@Bean
	<T extends ServerBuilder<T>> ServerBuilderCustomizer<T> executorServerConfigurer(
			GrpcServerExecutorProvider provider) {
		return new ServerBuilderCustomizerImplementation<>(provider);
	}

	private final class ServerBuilderCustomizerImplementation<T extends ServerBuilder<T>>
			implements ServerBuilderCustomizer<T>, Ordered {

		private final GrpcServerExecutorProvider provider;

		private ServerBuilderCustomizerImplementation(GrpcServerExecutorProvider provider) {
			this.provider = provider;
		}

		@Override
		public int getOrder() {
			return 0;
		}

		@Override
		public void customize(T builder) {
			builder.executor(this.provider.getExecutor());
		}

	}

	@ConditionalOnClass(name = "com.salesforce.reactivegrpc.common.Function")
	@Configuration
	@Import(ReactiveStubBeanDefinitionRegistrar.class)
	static class ReactiveStubConfiguration {

	}

}
