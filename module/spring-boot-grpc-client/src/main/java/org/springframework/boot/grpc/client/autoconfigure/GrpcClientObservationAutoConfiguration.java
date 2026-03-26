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
