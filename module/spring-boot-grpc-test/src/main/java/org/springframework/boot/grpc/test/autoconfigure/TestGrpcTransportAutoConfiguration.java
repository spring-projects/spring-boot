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

package org.springframework.boot.grpc.test.autoconfigure;

import io.grpc.BindableService;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.stub.AbstractStub;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.grpc.client.autoconfigure.GrpcClientAutoConfiguration;
import org.springframework.boot.grpc.server.autoconfigure.GrpcServerAutoConfiguration;
import org.springframework.boot.grpc.server.autoconfigure.GrpcServerProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.grpc.client.ClientInterceptorsConfigurer;
import org.springframework.grpc.client.GrpcChannelFactory;
import org.springframework.grpc.server.GrpcServerFactory;
import org.springframework.grpc.server.InProcessGrpcServerFactory;
import org.springframework.grpc.server.ServerServiceDefinitionFilter;
import org.springframework.grpc.server.lifecycle.GrpcServerLifecycle;
import org.springframework.grpc.server.service.GrpcServiceConfigurer;
import org.springframework.grpc.server.service.GrpcServiceDiscoverer;
import org.springframework.util.StringUtils;

/**
 * Auto-configuration for an in-process test gRPC transport.
 *
 * @author Chris Bono
 * @author Dave Syer
 * @author Andrey Litvitski
 * @author Phillip Webb
 * @author xing
 * @since 4.1.0
 * @see AutoConfigureTestGrpcTransport
 * @see TestGrpcTransportContextCustomizer
 */
@AutoConfiguration(before = { GrpcServerAutoConfiguration.class, GrpcClientAutoConfiguration.class })
@ConditionalOnClass({ InProcessServerBuilder.class, InProcessGrpcServerFactory.class })
public final class TestGrpcTransportAutoConfiguration {

	/**
	 * In-process address shared by the test server and channel factories within a single
	 * application context.
	 * <p>
	 * Prefer the test-only property written by {@link TestGrpcTransportContextCustomizer};
	 * otherwise generate a name for this context. Intentionally does not read
	 * {@code spring.grpc.server.inprocess.name}.
	 */
	record TestGrpcTransportAddress(String value) {
	}

	@Bean
	TestGrpcTransportAddress testGrpcTransportAddress(Environment environment) {
		String configured = environment.getProperty(TestGrpcTransportContextCustomizer.INPROCESS_NAME_PROPERTY);
		if (StringUtils.hasText(configured)) {
			return new TestGrpcTransportAddress(configured);
		}
		return new TestGrpcTransportAddress(InProcessServerBuilder.generateName());
	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnClass(GrpcServerFactory.class)
	@ConditionalOnBean(BindableService.class)
	static class TestGrpcServerTransportAutoConfiguration {

		@Bean
		@Order(Ordered.HIGHEST_PRECEDENCE)
		TestGrpcServerFactory testGrpcServerFactory(TestGrpcTransportAddress address,
				GrpcServiceDiscoverer serviceDiscoverer, GrpcServiceConfigurer serviceConfigurer,
				ObjectProvider<ServerServiceDefinitionFilter> serviceFilter) {
			TestGrpcServerFactory factory = new TestGrpcServerFactory(address.value());
			serviceFilter.ifAvailable(factory::setServiceFilter);
			serviceDiscoverer.findServices()
				.stream()
				.map((spec) -> serviceConfigurer.configure(spec, factory))
				.forEach(factory::addService);
			return factory;
		}

		@Bean
		@Order(Ordered.HIGHEST_PRECEDENCE)
		GrpcServerLifecycle testGrpcServerLifecycle(TestGrpcServerFactory testGrpcServerFactory,
				GrpcServerProperties properties, ApplicationEventPublisher eventPublisher) {
			return new GrpcServerLifecycle(testGrpcServerFactory, properties.getShutdown().getGracePeriod(),
					eventPublisher);
		}

	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnClass({ AbstractStub.class, GrpcChannelFactory.class })
	static class TestGrpcClientTransportAutoConfiguration {

		@Bean
		@ConditionalOnMissingBean
		ClientInterceptorsConfigurer grpcClientInterceptorsConfigurer(ApplicationContext applicationContext) {
			return new ClientInterceptorsConfigurer(applicationContext);
		}

		@Bean
		@Order(Ordered.HIGHEST_PRECEDENCE)
		TestGrpcChannelFactory testGrpcChannelFactory(TestGrpcTransportAddress address,
				ClientInterceptorsConfigurer interceptorsConfigurer) {
			return new TestGrpcChannelFactory(address.value(), interceptorsConfigurer);
		}

	}

}
