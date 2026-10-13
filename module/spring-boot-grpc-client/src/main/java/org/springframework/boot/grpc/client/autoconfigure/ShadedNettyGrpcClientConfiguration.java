/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.grpc.client.autoconfigure;

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
import org.springframework.grpc.client.ShadedNettyGrpcChannelFactory;

/**
 * {@link Configuration @Configuration} for a Shaded Netty gRPC client.
 *
 * @author Chris Bono
 * @author Phillip Webb
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass({ io.grpc.netty.shaded.io.netty.channel.Channel.class,
		io.grpc.netty.shaded.io.grpc.netty.NettyChannelBuilder.class })
@ConditionalOnMissingBean(value = GrpcChannelFactory.class, ignored = InProcessGrpcChannelFactory.class)
@ConditionalOnGrpcClientChannelFactoryEnabled
class ShadedNettyGrpcClientConfiguration {

	@Bean
	ShadedNettyGrpcChannelFactory shadedNettyGrpcChannelFactory(Environment environment,
			GrpcClientProperties properties, GrpcChannelBuilderCustomizers grpcChannelBuilderCustomizers,
			ClientInterceptorsConfigurer interceptorsConfigurer,
			ObjectProvider<GrpcChannelFactoryCustomizer> channelFactoryCustomizers,
			ChannelCredentialsProvider credentials) {
		ShadedNettyGrpcChannelFactory factory = new ShadedNettyGrpcChannelFactory(
				grpcChannelBuilderCustomizers.forFactory(), interceptorsConfigurer);
		factory.setCredentialsProvider(credentials);
		factory.setVirtualTargets(new PropertiesVirtualTargets(environment, properties));
		channelFactoryCustomizers.orderedStream().forEach((customizer) -> customizer.customize(factory));
		return factory;
	}

}
