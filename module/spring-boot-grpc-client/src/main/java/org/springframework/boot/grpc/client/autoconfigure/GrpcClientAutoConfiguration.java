/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.grpc.client.autoconfigure;

import io.grpc.CompressorRegistry;
import io.grpc.DecompressorRegistry;
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
