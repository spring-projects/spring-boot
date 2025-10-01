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

import io.micrometer.core.instrument.binder.grpc.ObservationGrpcServerInterceptor;
import io.micrometer.core.instrument.kotlin.ObservationCoroutineContextServerInterceptor;
import io.micrometer.observation.ObservationRegistry;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.grpc.server.GlobalServerInterceptor;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for gRPC server-side observations.
 *
 * @author Sunny Tang
 * @author Chris Bono
 * @author Dave Syer
 * @since 4.0.0
 */
@AutoConfiguration(
		afterName = "org.springframework.boot.micrometer.observation.autoconfigure.ObservationAutoConfiguration")
@ConditionalOnSpringGrpc
@ConditionalOnClass({ ObservationRegistry.class, ObservationGrpcServerInterceptor.class })
@ConditionalOnGrpcServerEnabled("observation")
@ConditionalOnBean(ObservationRegistry.class)
public final class GrpcServerObservationAutoConfiguration {

	@Bean
	@Order(0)
	@GlobalServerInterceptor
	ObservationGrpcServerInterceptor observationGrpcServerInterceptor(ObservationRegistry observationRegistry) {
		return new ObservationGrpcServerInterceptor(observationRegistry);
	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnClass(name = "io.grpc.kotlin.AbstractCoroutineStub")
	static class GrpcServerCoroutineStubConfiguration {

		@Bean
		@Order(10)
		@GlobalServerInterceptor
		ObservationCoroutineContextServerInterceptor observationCoroutineGrpcServerInterceptor(
				ObservationRegistry observationRegistry) {
			return new ObservationCoroutineContextServerInterceptor(observationRegistry);
		}

	}

}
