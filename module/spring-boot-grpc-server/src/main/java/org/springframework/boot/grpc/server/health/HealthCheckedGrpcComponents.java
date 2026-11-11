/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.grpc.server.health;

import java.util.Set;

import org.jspecify.annotations.Nullable;

/**
 * A collection of {@link HealthCheckedGrpcComponent components} used to check the health
 * of a gRPC server.
 *
 * @author Phillip Webb
 * @since 4.1.0
 * @see GrpcServerHealth
 */
public interface HealthCheckedGrpcComponents {

	/**
	 * Return the component that represents the overall server health or {@code null} if
	 * no overall health should be reported.
	 * @return the server component or {@code null}
	 */
	@Nullable HealthCheckedGrpcComponent getServer();

	/**
	 * Return the names of the services that contribute health checks.
	 * @return the service names
	 */
	Set<String> getServiceNames();

	/**
	 * Return the component for the service with the specified name or {@code null} if the
	 * name is not known.
	 * @param serviceName the name of the service
	 * @return the service component or {@code null}
	 */
	@Nullable HealthCheckedGrpcComponent getService(String serviceName);

}
