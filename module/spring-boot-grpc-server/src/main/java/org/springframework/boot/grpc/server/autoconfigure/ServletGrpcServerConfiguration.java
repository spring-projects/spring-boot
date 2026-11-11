/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.grpc.server.autoconfigure;

import io.grpc.servlet.jakarta.GrpcServlet;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication.Type;
import org.springframework.boot.diagnostics.FailureAnalyzedException;
import org.springframework.boot.grpc.server.GrpcServletRegistration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.grpc.server.service.GrpcServiceConfigurer;
import org.springframework.grpc.server.service.GrpcServiceDiscoverer;

/**
 * {@link Configuration @Configuration} for a Servlet gRPC server.
 *
 * @author David Syer
 * @author Chris Bono
 * @author Toshiaki Maki
 * @author Phillip Webb
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnWebApplication(type = Type.SERVLET)
@ConditionalOnClass(GrpcServlet.class)
@ConditionalOnMissingNetworkGrpcServer
@ConditionalOnBooleanProperty(name = "spring.grpc.server.servlet.enabled", matchIfMissing = true)
class ServletGrpcServerConfiguration {

	@Bean
	GrpcServletRegistration grpcServletRegistration(Environment environment, GrpcServerProperties properties,
			GrpcServiceDiscoverer serviceDiscoverer, GrpcServiceConfigurer serviceConfigurer,
			GrpcServerBuilderCustomizers grpcServerBuilderCustomizers) {
		if (properties.getServlet().isValidateHttp2()
				&& !Boolean.TRUE.equals(environment.getProperty("server.http2.enabled", Boolean.class))) {
			throw new FailureAnalyzedException(
					"Configuration property 'server.http2.enabled' should be set to true for gRPC support",
					"Update your application to correct the invalid configuration.\n"
							+ "You can also set 'spring.grpc.server.servlet.validate-http2' to false to disable the validation.");
		}
		return new GrpcServletRegistration(serviceDiscoverer, serviceConfigurer, grpcServerBuilderCustomizers::apply);
	}

}
