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

import java.util.List;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.TrustManagerFactory;

import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.netty.NettyServerBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.ssl.SslBundle;
import org.springframework.boot.ssl.SslBundles;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.grpc.server.GrpcServerFactory;
import org.springframework.grpc.server.InProcessGrpcServerFactory;
import org.springframework.grpc.server.NettyGrpcServerFactory;
import org.springframework.grpc.server.ServerBuilderCustomizer;
import org.springframework.grpc.server.ServerServiceDefinitionFilter;
import org.springframework.grpc.server.ShadedNettyGrpcServerFactory;
import org.springframework.grpc.server.lifecycle.GrpcServerLifecycle;
import org.springframework.grpc.server.service.GrpcServiceConfigurer;
import org.springframework.grpc.server.service.GrpcServiceDiscoverer;
import org.springframework.grpc.server.service.ServerInterceptorFilter;
import org.springframework.util.Assert;

/**
 * Configurations for {@link GrpcServerFactory gRPC server factories}.
 *
 * @author Chris Bono
 */
class GrpcServerFactoryConfigurations {

	private static void applyServerFactoryCustomizers(ObjectProvider<GrpcServerFactoryCustomizer> customizers,
			GrpcServerFactory factory) {
		customizers.orderedStream().forEach((customizer) -> customizer.customize(factory));
	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnClass(io.grpc.netty.shaded.io.grpc.netty.NettyServerBuilder.class)
	@ConditionalOnMissingBean(value = GrpcServerFactory.class, ignored = InProcessGrpcServerFactory.class)
	@ConditionalOnProperty(prefix = "spring.grpc.server.inprocess.", name = "exclusive", havingValue = "false",
			matchIfMissing = true)
	@EnableConfigurationProperties(GrpcServerProperties.class)
	static class ShadedNettyServerFactoryConfiguration {

		@Bean
		ShadedNettyGrpcServerFactory shadedNettyGrpcServerFactory(GrpcServerProperties properties,
				GrpcServiceDiscoverer serviceDiscoverer, GrpcServiceConfigurer serviceConfigurer,
				ServerBuilderCustomizers serverBuilderCustomizers, SslBundles bundles,
				ObjectProvider<GrpcServerFactoryCustomizer> customizers) {
			ShadedNettyServerFactoryPropertyMapper mapper = new ShadedNettyServerFactoryPropertyMapper(properties);
			List<ServerBuilderCustomizer<io.grpc.netty.shaded.io.grpc.netty.NettyServerBuilder>> builderCustomizers = List
				.of(mapper::customizeServerBuilder, serverBuilderCustomizers::customize);
			KeyManagerFactory keyManager = null;
			TrustManagerFactory trustManager = null;
			if (properties.getSsl().isEnabled()) {
				String bundleName = properties.getSsl().getBundle();
				Assert.notNull(bundleName, () -> "SSL bundleName must not be null");
				SslBundle bundle = bundles.getBundle(bundleName);
				keyManager = bundle.getManagers().getKeyManagerFactory();
				trustManager = properties.getSsl().isSecure() ? bundle.getManagers().getTrustManagerFactory()
						: io.grpc.netty.shaded.io.netty.handler.ssl.util.InsecureTrustManagerFactory.INSTANCE;
			}
			ShadedNettyGrpcServerFactory factory = new ShadedNettyGrpcServerFactory(properties.getAddress(),
					builderCustomizers, keyManager, trustManager, properties.getSsl().getClientAuth());
			applyServerFactoryCustomizers(customizers, factory);
			serviceDiscoverer.findServices()
				.stream()
				.map((serviceSpec) -> serviceConfigurer.configure(serviceSpec, factory))
				.forEach(factory::addService);
			return factory;
		}

		@ConditionalOnBean(ShadedNettyGrpcServerFactory.class)
		@ConditionalOnMissingBean(name = "shadedNettyGrpcServerLifecycle")
		@Bean
		GrpcServerLifecycle shadedNettyGrpcServerLifecycle(ShadedNettyGrpcServerFactory factory,
				GrpcServerProperties properties, ApplicationEventPublisher eventPublisher) {
			return new GrpcServerLifecycle(factory, properties.getShutdownGracePeriod(), eventPublisher);
		}

	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnClass(NettyServerBuilder.class)
	@ConditionalOnMissingBean(value = GrpcServerFactory.class, ignored = InProcessGrpcServerFactory.class)
	@ConditionalOnProperty(prefix = "spring.grpc.server.inprocess.", name = "exclusive", havingValue = "false",
			matchIfMissing = true)
	@EnableConfigurationProperties(GrpcServerProperties.class)
	static class NettyServerFactoryConfiguration {

		@Bean
		NettyGrpcServerFactory nettyGrpcServerFactory(GrpcServerProperties properties,
				GrpcServiceDiscoverer serviceDiscoverer, GrpcServiceConfigurer serviceConfigurer,
				ServerBuilderCustomizers serverBuilderCustomizers, SslBundles bundles,
				ObjectProvider<GrpcServerFactoryCustomizer> customizers) {
			NettyServerFactoryPropertyMapper mapper = new NettyServerFactoryPropertyMapper(properties);
			List<ServerBuilderCustomizer<NettyServerBuilder>> builderCustomizers = List
				.of(mapper::customizeServerBuilder, serverBuilderCustomizers::customize);
			KeyManagerFactory keyManager = null;
			TrustManagerFactory trustManager = null;
			if (properties.getSsl().isEnabled()) {
				String bundleName = properties.getSsl().getBundle();
				Assert.notNull(bundleName, () -> "SSL bundleName must not be null");
				SslBundle bundle = bundles.getBundle(bundleName);
				keyManager = bundle.getManagers().getKeyManagerFactory();
				trustManager = properties.getSsl().isSecure() ? bundle.getManagers().getTrustManagerFactory()
						: InsecureTrustManagerFactory.INSTANCE;
			}
			NettyGrpcServerFactory factory = new NettyGrpcServerFactory(properties.getAddress(), builderCustomizers,
					keyManager, trustManager, properties.getSsl().getClientAuth());
			applyServerFactoryCustomizers(customizers, factory);
			serviceDiscoverer.findServices()
				.stream()
				.map((serviceSpec) -> serviceConfigurer.configure(serviceSpec, factory))
				.forEach(factory::addService);
			return factory;
		}

		@ConditionalOnBean(NettyGrpcServerFactory.class)
		@ConditionalOnMissingBean(name = "nettyGrpcServerLifecycle")
		@Bean
		GrpcServerLifecycle nettyGrpcServerLifecycle(NettyGrpcServerFactory factory, GrpcServerProperties properties,
				ApplicationEventPublisher eventPublisher) {
			return new GrpcServerLifecycle(factory, properties.getShutdownGracePeriod(), eventPublisher);
		}

	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnClass(InProcessGrpcServerFactory.class)
	@ConditionalOnMissingBean(InProcessGrpcServerFactory.class)
	@ConditionalOnProperty(prefix = "spring.grpc.server.inprocess", name = "name")
	@EnableConfigurationProperties(GrpcServerProperties.class)
	static class InProcessServerFactoryConfiguration {

		@Bean
		InProcessGrpcServerFactory inProcessGrpcServerFactory(GrpcServerProperties properties,
				GrpcServiceDiscoverer serviceDiscoverer, GrpcServiceConfigurer serviceConfigurer,
				ServerBuilderCustomizers serverBuilderCustomizers,
				ObjectProvider<ServerInterceptorFilter> interceptorFilter,
				ObjectProvider<ServerServiceDefinitionFilter> serviceFilter,
				ObjectProvider<GrpcServerFactoryCustomizer> customizers) {
			var mapper = new InProcessServerFactoryPropertyMapper(properties);
			List<ServerBuilderCustomizer<InProcessServerBuilder>> builderCustomizers = List
				.of(mapper::customizeServerBuilder, serverBuilderCustomizers::customize);
			InProcessGrpcServerFactory factory = new InProcessGrpcServerFactory(properties.getInprocess().getName(),
					builderCustomizers);
			factory.setInterceptorFilter(interceptorFilter.getIfAvailable());
			factory.setServiceFilter(serviceFilter.getIfAvailable());
			applyServerFactoryCustomizers(customizers, factory);
			serviceDiscoverer.findServices()
				.stream()
				.map((serviceSpec) -> serviceConfigurer.configure(serviceSpec, factory))
				.forEach(factory::addService);
			return factory;
		}

		@ConditionalOnBean(InProcessGrpcServerFactory.class)
		@ConditionalOnMissingBean(name = "inProcessGrpcServerLifecycle")
		@Bean
		GrpcServerLifecycle inProcessGrpcServerLifecycle(InProcessGrpcServerFactory factory,
				GrpcServerProperties properties, ApplicationEventPublisher eventPublisher) {
			return new GrpcServerLifecycle(factory, properties.getShutdownGracePeriod(), eventPublisher);
		}

	}

}
