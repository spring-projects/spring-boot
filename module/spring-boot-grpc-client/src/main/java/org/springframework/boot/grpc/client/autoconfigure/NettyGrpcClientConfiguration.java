/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.grpc.client.autoconfigure;

import io.grpc.Channel;
import io.grpc.netty.NettyChannelBuilder;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.grpc.client.ChannelCredentialsProvider;
import org.springframework.grpc.client.ClientInterceptorsConfigurer;
import org.springframework.grpc.client.GrpcChannelFactory;
import org.springframework.grpc.client.InProcessGrpcChannelFactory;
import org.springframework.grpc.client.NettyGrpcChannelFactory;

/**
 * {@link Configuration @Configuration} for a Netty gRPC client.
 *
 * @author Chris Bono
 * @author Phillip Webb
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass({ Channel.class, NettyChannelBuilder.class })
@ConditionalOnMissingBean(value = GrpcChannelFactory.class, ignored = InProcessGrpcChannelFactory.class)
@ConditionalOnGrpcClientChannelFactoryEnabled
class NettyGrpcClientConfiguration {

	@Bean
	NettyGrpcChannelFactory nettyGrpcChannelFactory(Environment environment, GrpcClientProperties properties,
			GrpcChannelBuilderCustomizers grpcChannelBuilderCustomizers,
			ClientInterceptorsConfigurer interceptorsConfigurer,
			ObjectProvider<GrpcChannelFactoryCustomizer> channelFactoryCustomizers,
			ChannelCredentialsProvider credentials) {
		NettyGrpcChannelFactory factory = new NettyGrpcChannelFactory(grpcChannelBuilderCustomizers.forFactory(),
				interceptorsConfigurer);
		factory.setCredentialsProvider(credentials);
		factory.setVirtualTargets(new PropertiesVirtualTargets(environment, properties));
		channelFactoryCustomizers.orderedStream().forEach((customizer) -> customizer.customize(factory));
		return factory;
	}

}
