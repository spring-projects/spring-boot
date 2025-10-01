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

import java.util.List;

import io.grpc.Channel;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.netty.NettyChannelBuilder;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.grpc.client.ChannelCredentialsProvider;
import org.springframework.grpc.client.ClientInterceptorFilter;
import org.springframework.grpc.client.ClientInterceptorsConfigurer;
import org.springframework.grpc.client.GrpcChannelBuilderCustomizer;
import org.springframework.grpc.client.GrpcChannelFactory;
import org.springframework.grpc.client.InProcessGrpcChannelFactory;
import org.springframework.grpc.client.NettyGrpcChannelFactory;
import org.springframework.grpc.client.ShadedNettyGrpcChannelFactory;

/**
 * Configurations for {@link GrpcChannelFactory gRPC channel factories}.
 *
 * @author Chris Bono
 */
class GrpcChannelFactoryConfigurations {

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnClass({ io.grpc.netty.shaded.io.netty.channel.Channel.class,
			io.grpc.netty.shaded.io.grpc.netty.NettyChannelBuilder.class })
	@ConditionalOnMissingBean(value = GrpcChannelFactory.class, ignored = InProcessGrpcChannelFactory.class)
	@ConditionalOnProperty(prefix = "spring.grpc.client.inprocess.", name = "exclusive", havingValue = "false",
			matchIfMissing = true)
	@EnableConfigurationProperties(GrpcClientProperties.class)
	static class ShadedNettyChannelFactoryConfiguration {

		@Bean
		ShadedNettyGrpcChannelFactory shadedNettyGrpcChannelFactory(GrpcClientProperties properties,
				ChannelBuilderCustomizers channelBuilderCustomizers,
				ClientInterceptorsConfigurer interceptorsConfigurer,
				ObjectProvider<GrpcChannelFactoryCustomizer> channelFactoryCustomizers,
				ChannelCredentialsProvider credentials) {
			List<GrpcChannelBuilderCustomizer<io.grpc.netty.shaded.io.grpc.netty.NettyChannelBuilder>> builderCustomizers = List
				.of(channelBuilderCustomizers::customize);
			ShadedNettyGrpcChannelFactory factory = new ShadedNettyGrpcChannelFactory(builderCustomizers,
					interceptorsConfigurer);
			factory.setCredentialsProvider(credentials);
			factory.setVirtualTargets(properties);
			channelFactoryCustomizers.orderedStream().forEach((customizer) -> customizer.customize(factory));
			return factory;
		}

	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnClass({ Channel.class, NettyChannelBuilder.class })
	@ConditionalOnMissingBean(value = GrpcChannelFactory.class, ignored = InProcessGrpcChannelFactory.class)
	@ConditionalOnProperty(prefix = "spring.grpc.client.inprocess.", name = "exclusive", havingValue = "false",
			matchIfMissing = true)
	@EnableConfigurationProperties(GrpcClientProperties.class)
	static class NettyChannelFactoryConfiguration {

		@Bean
		NettyGrpcChannelFactory nettyGrpcChannelFactory(GrpcClientProperties properties,
				ChannelBuilderCustomizers channelBuilderCustomizers,
				ClientInterceptorsConfigurer interceptorsConfigurer,
				ObjectProvider<GrpcChannelFactoryCustomizer> channelFactoryCustomizers,
				ChannelCredentialsProvider credentials) {
			List<GrpcChannelBuilderCustomizer<NettyChannelBuilder>> builderCustomizers = List
				.of(channelBuilderCustomizers::customize);
			NettyGrpcChannelFactory factory = new NettyGrpcChannelFactory(builderCustomizers, interceptorsConfigurer);
			factory.setCredentialsProvider(credentials);
			factory.setVirtualTargets(properties);
			channelFactoryCustomizers.orderedStream().forEach((customizer) -> customizer.customize(factory));
			return factory;
		}

	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnClass(InProcessChannelBuilder.class)
	@ConditionalOnMissingBean(InProcessGrpcChannelFactory.class)
	@ConditionalOnProperty(prefix = "spring.grpc.client.inprocess", name = "enabled", havingValue = "true",
			matchIfMissing = true)
	static class InProcessChannelFactoryConfiguration {

		@Bean
		InProcessGrpcChannelFactory inProcessGrpcChannelFactory(ChannelBuilderCustomizers channelBuilderCustomizers,
				ClientInterceptorsConfigurer interceptorsConfigurer,
				ObjectProvider<ClientInterceptorFilter> interceptorFilter,
				ObjectProvider<GrpcChannelFactoryCustomizer> channelFactoryCustomizers) {
			List<GrpcChannelBuilderCustomizer<InProcessChannelBuilder>> inProcessBuilderCustomizers = List
				.of(channelBuilderCustomizers::customize);
			InProcessGrpcChannelFactory factory = new InProcessGrpcChannelFactory(inProcessBuilderCustomizers,
					interceptorsConfigurer);
			if (interceptorFilter != null) {
				factory.setInterceptorFilter(interceptorFilter.getIfAvailable(() -> null));
			}
			channelFactoryCustomizers.orderedStream().forEach((customizer) -> customizer.customize(factory));
			return factory;
		}

	}

}
