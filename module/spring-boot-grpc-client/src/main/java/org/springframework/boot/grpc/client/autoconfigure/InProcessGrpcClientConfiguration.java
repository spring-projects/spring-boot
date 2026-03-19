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

import io.grpc.inprocess.InProcessChannelBuilder;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.grpc.client.ClientInterceptorFilter;
import org.springframework.grpc.client.ClientInterceptorsConfigurer;
import org.springframework.grpc.client.InProcessGrpcChannelFactory;

/**
 * {@link Configuration @Configuration} for an in-process gRPC client.
 *
 * @author Chris Bono
 * @author Phillip Webb
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass(InProcessChannelBuilder.class)
@ConditionalOnMissingBean(InProcessGrpcChannelFactory.class)
@ConditionalOnGrpcClientChannelFactoryEnabled
@ConditionalOnProperty(name = "spring.grpc.client.inprocess.enabled", havingValue = "true", matchIfMissing = true)
class InProcessGrpcClientConfiguration {

	@Bean
	InProcessGrpcChannelFactory inProcessGrpcChannelFactory(GrpcChannelBuilderCustomizers grpcChannelBuilderCustomizers,
			ClientInterceptorsConfigurer interceptorsConfigurer,
			ObjectProvider<ClientInterceptorFilter> interceptorFilter,
			ObjectProvider<GrpcChannelFactoryCustomizer> channelFactoryCustomizers) {
		InProcessGrpcChannelFactory factory = new InProcessGrpcChannelFactory(
				grpcChannelBuilderCustomizers.forFactory(), interceptorsConfigurer);
		interceptorFilter.ifAvailable(factory::setInterceptorFilter);
		channelFactoryCustomizers.orderedStream().forEach((customizer) -> customizer.customize(factory));
		return factory;
	}

}
