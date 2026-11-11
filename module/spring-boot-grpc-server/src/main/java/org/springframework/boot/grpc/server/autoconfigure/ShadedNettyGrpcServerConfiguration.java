/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.grpc.server.autoconfigure;

import io.grpc.netty.shaded.io.grpc.netty.NettyServerBuilder;
import io.grpc.netty.shaded.io.netty.handler.ssl.util.InsecureTrustManagerFactory;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.ssl.SslBundles;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.grpc.server.ShadedNettyGrpcServerFactory;
import org.springframework.grpc.server.lifecycle.GrpcServerLifecycle;
import org.springframework.grpc.server.service.GrpcServiceConfigurer;
import org.springframework.grpc.server.service.GrpcServiceDiscoverer;

/**
 * {@link Configuration @Configuration} for a Shaded Netty gRPC server.
 *
 * @author David Syer
 * @author Chris Bono
 * @author Toshiaki Maki
 * @author Phillip Webb
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass(NettyServerBuilder.class)
@ConditionalOnMissingNetworkGrpcServer
@ConditionalOnGrpcServerFactoryEnabled
class ShadedNettyGrpcServerConfiguration {

	@Bean
	ShadedNettyGrpcServerFactory shadedNettyGrpcServerFactory(GrpcServerProperties properties,
			GrpcServiceDiscoverer serviceDiscoverer, GrpcServiceConfigurer serviceConfigurer,
			GrpcServerBuilderCustomizers grpcServerBuilderCustomizers, SslBundles bundles,
			ObjectProvider<GrpcServerFactoryCustomizer> customizers) {
		NettyAddress address = NettyAddress.fromProperties(properties);
		ServerCredentials serverCredentials = ServerCredentials.get(properties.getSsl(), bundles,
				InsecureTrustManagerFactory.INSTANCE);
		ShadedNettyGrpcServerFactory factory = new ShadedNettyGrpcServerFactory(address.toString(),
				grpcServerBuilderCustomizers.forFactory(), serverCredentials.keyManagerFactory(),
				serverCredentials.trustManagerFactory(), serverCredentials.clientAuth());
		customizers.orderedStream().forEach((customizer) -> customizer.customize(factory));
		serviceDiscoverer.findServices()
			.stream()
			.map((spec) -> serviceConfigurer.configure(spec, factory))
			.forEach(factory::addService);
		return factory;
	}

	@Bean
	@ConditionalOnMissingBean(name = "shadedNettyGrpcServerLifecycle")
	GrpcServerLifecycle shadedNettyGrpcServerLifecycle(ShadedNettyGrpcServerFactory factory,
			GrpcServerProperties properties, ApplicationEventPublisher eventPublisher) {
		return new GrpcServerLifecycle(factory, properties.getShutdown().getGracePeriod(), eventPublisher);
	}

}
