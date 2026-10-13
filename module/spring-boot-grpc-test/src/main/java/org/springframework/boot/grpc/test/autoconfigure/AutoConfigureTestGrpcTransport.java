/*
 * Copyright 2012-present the original author or authors.
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
