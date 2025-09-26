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

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.context.annotation.Conditional;

/**
 * {@link Conditional @Conditional} that checks whether the gRPC server and optionally a
 * specific gRPC service is enabled. It matches if the value of the
 * {@code spring.grpc.server.enabled} property is not explicitly set to {@code false} and
 * if the {@link #value() gRPC service name} is set, that the
 * {@code spring.grpc.server.<service-name>.enabled} property is not explicitly set to
 * {@code false}.
 *
 * @author Freeman Freeman
 * @author Chris Bono
 * @since 4.0.0
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE, ElementType.METHOD })
@Conditional(OnEnabledGrpcServerCondition.class)
public @interface ConditionalOnGrpcServerEnabled {

	/**
	 * Name of the gRPC service.
	 * @return the name of the gRPC service
	 */
	String value() default "";

}
