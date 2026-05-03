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
