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

import io.micrometer.core.instrument.binder.grpc.ObservationGrpcClientInterceptor;
import io.micrometer.observation.ObservationRegistry;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.grpc.client.GlobalClientInterceptor;

@AutoConfiguration(
		afterName = "org.springframework.boot.micrometer.observation.autoconfigure.ObservationAutoConfiguration")
@ConditionalOnGrpcClientEnabled
@ConditionalOnClass({ ObservationRegistry.class, ObservationGrpcClientInterceptor.class })
@ConditionalOnBean(ObservationRegistry.class)
@ConditionalOnProperty(name = "spring.grpc.client.observation.enabled", havingValue = "true", matchIfMissing = true)

public final class GrpcClientObservationAutoConfiguration {

	@Bean
	@GlobalClientInterceptor
	@ConditionalOnMissingBean
	ObservationGrpcClientInterceptor observationGrpcClientInterceptor(ObservationRegistry observationRegistry) {
		return new ObservationGrpcClientInterceptor(observationRegistry);
	}

}
