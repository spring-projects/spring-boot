/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.grpc.client.autoconfigure;

import io.grpc.stub.AbstractStub;
import io.micrometer.core.instrument.binder.grpc.GrpcClientObservationConvention;
import io.micrometer.core.instrument.binder.grpc.ObservationGrpcClientInterceptor;
import io.micrometer.observation.ObservationRegistry;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.grpc.client.GlobalClientInterceptor;
import org.springframework.grpc.client.GrpcChannelBuilderCustomizer;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for gRPC observation support.
 *
 * @author Chris Bono
 * @author Phillip Webb
 * @since 4.1.0
 */
@AutoConfiguration(
		afterName = "org.springframework.boot.micrometer.observation.autoconfigure.ObservationAutoConfiguration")
@ConditionalOnClass({ AbstractStub.class, GrpcChannelBuilderCustomizer.class, ObservationRegistry.class,
		ObservationGrpcClientInterceptor.class })
@ConditionalOnProperty(name = "spring.grpc.client.enabled", matchIfMissing = true)
@ConditionalOnProperty(name = "spring.grpc.client.observation.enabled", havingValue = "true", matchIfMissing = true)
@ConditionalOnBean(ObservationRegistry.class)
public final class GrpcClientObservationAutoConfiguration {

	@Bean
	@GlobalClientInterceptor
	@ConditionalOnMissingBean
	ObservationGrpcClientInterceptor grpcClientObservationInterceptor(ObservationRegistry observationRegistry,
			ObjectProvider<GrpcClientObservationConvention> customConvention) {
		ObservationGrpcClientInterceptor interceptor = new ObservationGrpcClientInterceptor(observationRegistry);
		customConvention.ifAvailable(interceptor::setCustomConvention);
		return interceptor;
	}

}
