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

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
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

@AutoConfiguration(before = CompositeChannelFactoryAutoConfiguration.class)
@ConditionalOnGrpcClientEnabled
@EnableConfigurationProperties(GrpcClientProperties.class)
@Import({ GrpcCodecConfiguration.class, GrpcChannelFactoryConfigurations.ShadedNettyChannelFactoryConfiguration.class,
		GrpcChannelFactoryConfigurations.NettyChannelFactoryConfiguration.class,
		GrpcChannelFactoryConfigurations.InProcessChannelFactoryConfiguration.class, ClientScanConfiguration.class })
public final class GrpcClientAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean
	ClientInterceptorsConfigurer clientInterceptorsConfigurer(ApplicationContext applicationContext) {
		return new ClientInterceptorsConfigurer(applicationContext);
	}

	@Bean
	@ConditionalOnMissingBean(ChannelCredentialsProvider.class)
	NamedChannelCredentialsProvider channelCredentialsProvider(SslBundles bundles, GrpcClientProperties properties) {
		return new NamedChannelCredentialsProvider(bundles, properties);
	}

	@Bean
	<T extends ManagedChannelBuilder<T>> GrpcChannelBuilderCustomizer<T> clientPropertiesChannelCustomizer(
			GrpcClientProperties properties) {
		return new ClientPropertiesChannelBuilderCustomizer<>(properties);
	}

	@ConditionalOnBean(CompressorRegistry.class)
	@Bean
	<T extends ManagedChannelBuilder<T>> GrpcChannelBuilderCustomizer<T> compressionClientCustomizer(
			CompressorRegistry registry) {
		return (name, builder) -> builder.compressorRegistry(registry);
	}

	@ConditionalOnBean(DecompressorRegistry.class)
	@Bean
	<T extends ManagedChannelBuilder<T>> GrpcChannelBuilderCustomizer<T> decompressionClientCustomizer(
			DecompressorRegistry registry) {
		return (name, builder) -> builder.decompressorRegistry(registry);
	}

	@ConditionalOnMissingBean
	@Bean
	ChannelBuilderCustomizers channelBuilderCustomizers(ObjectProvider<GrpcChannelBuilderCustomizer<?>> customizers) {
		return new ChannelBuilderCustomizers(customizers.orderedStream().toList());
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
