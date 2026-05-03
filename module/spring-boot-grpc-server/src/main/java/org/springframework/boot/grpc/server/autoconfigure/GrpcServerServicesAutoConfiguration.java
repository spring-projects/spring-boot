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

import io.grpc.BindableService;
import io.grpc.protobuf.services.ProtoReflectionServiceV1;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.grpc.server.GrpcServerFactory;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for gRPC server services.
 *
 * @author Haris Zujo
 * @author Dave Syer
 * @author Chris Bono
 * @author Andrey Litvitski
 * @since 4.1.0
 */
@AutoConfiguration(before = GrpcServerAutoConfiguration.class)
@ConditionalOnClass({ GrpcServerFactory.class, io.grpc.Grpc.class })
@ConditionalOnBooleanProperty(name = "spring.grpc.server.enabled", matchIfMissing = true)
public final class GrpcServerServicesAutoConfiguration {

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnClass(ProtoReflectionServiceV1.class)
	@ConditionalOnBean(BindableService.class)
	@ConditionalOnBooleanProperty(name = "spring.grpc.server.reflection.enabled", matchIfMissing = true)
	static class GrpcServerReflectionServiceConfiguration {

		@Bean
		BindableService grpcServerReflectionService() {
			return ProtoReflectionServiceV1.newInstance();
		}

	}

}
