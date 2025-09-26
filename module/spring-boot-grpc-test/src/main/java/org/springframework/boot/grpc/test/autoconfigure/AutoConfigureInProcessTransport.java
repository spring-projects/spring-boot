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

package org.springframework.boot.grpc.test.autoconfigure;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.test.autoconfigure.properties.PropertyMapping;

/**
 * Annotation that can be applied to a test class to start an in-process gRPC server. All
 * clients that connect to any server via the autoconfigured {@code GrpcChannelFactory}
 * will be able to connect to the in-process gRPC server.
 *
 * @author Dave Syer
 * @author Chris Bono
 * @since 4.0.0
 * @see InProcessTestAutoConfiguration
 */
@Target({ ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@ImportAutoConfiguration
@PropertyMapping("spring.test.grpc.inprocess")
public @interface AutoConfigureInProcessTransport {

	/**
	 * Whether to start an in-process test gRPC server. Defaults to {@code true}.
	 * @return whether to start an in-process gRPC server
	 */
	@SuppressWarnings("unused")
	boolean enabled() default true;

}
