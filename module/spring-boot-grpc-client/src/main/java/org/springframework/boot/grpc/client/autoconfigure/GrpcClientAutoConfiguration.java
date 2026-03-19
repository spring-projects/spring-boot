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

package org.springframework.boot.grpc.client.autoconfigure;

import io.grpc.CompressorRegistry;
import io.grpc.DecompressorRegistry;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.AbstractStub;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.ssl.SslBundles;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.grpc.client.ChannelCredentialsProvider;
import org.springframework.grpc.client.ClientInterceptorsConfigurer;
import org.springframework.grpc.client.CoroutineStubFactory;
import org.springframework.grpc.client.GrpcChannelBuilderCustomizer;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for gRPC clients.
 *
 * @author Dave Syer
 * @author Chris Bono
 * @author Phillip Webb
 * @since 4.1.0
 */
@AutoConfiguration(before = CompositeChannelFactoryAutoConfiguration.class)
@ConditionalOnClass({ AbstractStub.class, GrpcChannelBuilderCustomizer.class })
@ConditionalOnProperty(name = "spring.grpc.client.enabled", matchIfMissing = true)
@EnableConfigurationProperties(GrpcClientProperties.class)
@Import({ GrpcClientCodecConfiguration.class, ShadedNettyGrpcClientConfiguration.class,
		NettyGrpcClientConfiguration.class, InProcessGrpcClientConfiguration.class })
public final class GrpcClientAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean
	ClientInterceptorsConfigurer grpcClientInterceptorsConfigurer(ApplicationContext applicationContext) {
		return new ClientInterceptorsConfigurer(applicationContext);
	}

	@Bean
	@ConditionalOnMissingBean(ChannelCredentialsProvider.class)
	PropertiesChannelCredentialsProvider grpcChannelCredentialsProvider(SslBundles bundles,
			GrpcClientProperties properties) {
		return new PropertiesChannelCredentialsProvider(properties, bundles);
	}

	@Bean
	<T extends ManagedChannelBuilder<T>> PropertiesGrpcChannelBuilderCustomizer<T> grpcClientPropertiesChannelCustomizer(
			GrpcClientProperties properties) {
		return new PropertiesGrpcChannelBuilderCustomizer<>(properties);
	}

	@Bean
	GrpcChannelBuilderCustomizers grpcDefaultServicesChannelBuilderCustomizer(GrpcClientProperties grpcClientProperties,
			ObjectProvider<CompressorRegistry> compressorRegistry,
			ObjectProvider<DecompressorRegistry> decompressorRegistry,
			ObjectProvider<GrpcChannelBuilderCustomizer<?>> customizers,
			ObjectProvider<GrpcClientDefaultServiceConfigCustomizer> defaultServiceConfigCustomizers) {
		return new GrpcChannelBuilderCustomizers(grpcClientProperties, compressorRegistry, decompressorRegistry,
				customizers, defaultServiceConfigCustomizers);
	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnClass(name = "io.grpc.kotlin.AbstractCoroutineStub")
	static class GrpcClientCoroutineStubConfiguration {

		@Bean
		@ConditionalOnMissingBean
		CoroutineStubFactory coroutineStubFactory() {
			return new CoroutineStubFactory();
		}

	}

}
