/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.grpc.server.autoconfigure;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.grpc.server.InProcessGrpcServerFactory;
import org.springframework.grpc.server.lifecycle.GrpcServerLifecycle;
import org.springframework.grpc.server.service.GrpcServiceConfigurer;
import org.springframework.grpc.server.service.GrpcServiceDiscoverer;
import org.springframework.util.Assert;

/**
 * {@link Configuration @Configuration} for an in-process gRPC server.
 *
 * @author David Syer
 * @author Chris Bono
 * @author Toshiaki Maki
 * @author Phillip Webb
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnMissingBean(InProcessGrpcServerFactory.class)
@ConditionalOnProperty("spring.grpc.server.inprocess.name")
@ConditionalOnGrpcServerFactoryEnabled
class InProcessGrpcServerConfiguration {

	@Bean
	InProcessGrpcServerFactory inProcessGrpcServerFactory(GrpcServerProperties properties,
			GrpcServiceDiscoverer serviceDiscoverer, GrpcServiceConfigurer serviceConfigurer,
			GrpcServerBuilderCustomizers grpcServerBuilderCustomizers,
			ObjectProvider<GrpcServerFactoryCustomizer> customizers) {
		String inProcessName = properties.getInprocess().getName();
		Assert.state(inProcessName != null, "No inprocess name provided");
		InProcessGrpcServerFactory factory = new InProcessGrpcServerFactory(inProcessName,
				grpcServerBuilderCustomizers.forFactory());
		customizers.orderedStream().forEach((customizer) -> customizer.customize(factory));
		serviceDiscoverer.findServices()
			.stream()
			.map((spec) -> serviceConfigurer.configure(spec, factory))
			.forEach(factory::addService);
		return factory;
	}

	@Bean
	@ConditionalOnBean(InProcessGrpcServerFactory.class)
	@ConditionalOnMissingBean(name = "inProcessGrpcServerLifecycle")
	GrpcServerLifecycle inProcessGrpcServerLifecycle(InProcessGrpcServerFactory factory,
			GrpcServerProperties properties, ApplicationEventPublisher eventPublisher) {
		return new GrpcServerLifecycle(factory, properties.getShutdown().getGracePeriod(), eventPublisher);
	}

}
