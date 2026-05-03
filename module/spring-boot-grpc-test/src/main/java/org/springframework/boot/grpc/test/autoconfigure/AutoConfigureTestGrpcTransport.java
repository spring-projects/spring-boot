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
import org.springframework.boot.grpc.server.GrpcServletRegistration;
import org.springframework.boot.test.context.PropertyMapping;
import org.springframework.grpc.client.GrpcChannelFactory;
import org.springframework.grpc.server.GrpcServerFactory;

/**
 * Annotation that can be applied to a test class to enable test in-process gRPC
 * transport. Starts a test in-process gRPC server and configures a
 * {@code GrpcChannelFactory} that will connect all targets to it.
 *
 * @author Dave Syer
 * @author Chris Bono
 * @author Phillip Webb
 * @since 4.1.0
 * @see TestGrpcTransportAutoConfiguration
 */
@Target({ ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@ImportAutoConfiguration
public @interface AutoConfigureTestGrpcTransport {

	/**
	 * Enables auto-configuration of the {@link GrpcServletRegistration}. Defaults to
	 * {@code false} since servlet registration is unnecessary when using test in-process
	 * transport.
	 * @return if servlet support is enabled
	 */
	@PropertyMapping("spring.grpc.server.servlet.enabled")
	boolean enableServlet() default false;

	/**
	 * Enables auto-configuration of {@link GrpcServerFactory} beans. Defaults to
	 * {@code false} since additional server factories are unnecessary when using test
	 * in-process transport.
	 * @return if server factories are enabled
	 */
	@PropertyMapping("spring.grpc.server.factory.enabled")
	boolean enableServerFactory() default false;

	/**
	 * Enables auto-configuration of {@link GrpcChannelFactory} beans. Defaults to
	 * {@code false} since additional channel factories are unnecessary when using test
	 * in-process transport.
	 * @return if channel factories are enabled
	 */
	@PropertyMapping("spring.grpc.client.channelfactory.enabled")
	boolean enableChannelFactory() default false;

}
