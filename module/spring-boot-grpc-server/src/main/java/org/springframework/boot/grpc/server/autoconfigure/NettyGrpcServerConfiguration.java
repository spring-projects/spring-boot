/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.grpc.server.autoconfigure;

import io.grpc.netty.NettyServerBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.ssl.SslBundles;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.grpc.server.NettyGrpcServerFactory;
import org.springframework.grpc.server.lifecycle.GrpcServerLifecycle;
import org.springframework.grpc.server.service.GrpcServiceConfigurer;
import org.springframework.grpc.server.service.GrpcServiceDiscoverer;

/**
 * {@link Configuration @Configuration} for a Netty gRPC server.
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
class NettyGrpcServerConfiguration {

	@Bean
	NettyGrpcServerFactory nettyGrpcServerFactory(GrpcServerProperties properties,
			GrpcServiceDiscoverer serviceDiscoverer, GrpcServiceConfigurer serviceConfigurer,
			GrpcServerBuilderCustomizers grpcServerBuilderCustomizers, SslBundles bundles,
			ObjectProvider<GrpcServerFactoryCustomizer> customizers) {
		NettyAddress address = NettyAddress.fromProperties(properties);
		ServerCredentials credentials = ServerCredentials.get(properties.getSsl(), bundles,
				InsecureTrustManagerFactory.INSTANCE);
		NettyGrpcServerFactory factory = new NettyGrpcServerFactory(address.toString(),
				grpcServerBuilderCustomizers.forFactory(), credentials.keyManagerFactory(),
				credentials.trustManagerFactory(), credentials.clientAuth());
		customizers.orderedStream().forEach((customizer) -> customizer.customize(factory));
		serviceDiscoverer.findServices()
			.stream()
			.map((spec) -> serviceConfigurer.configure(spec, factory))
			.forEach(factory::addService);
		return factory;
	}

	@Bean
	@ConditionalOnMissingBean(name = "nettyGrpcServerLifecycle")
	GrpcServerLifecycle nettyGrpcServerLifecycle(NettyGrpcServerFactory factory, GrpcServerProperties properties,
			ApplicationEventPublisher eventPublisher) {
		return new GrpcServerLifecycle(factory, properties.getShutdown().getGracePeriod(), eventPublisher);
	}

}
