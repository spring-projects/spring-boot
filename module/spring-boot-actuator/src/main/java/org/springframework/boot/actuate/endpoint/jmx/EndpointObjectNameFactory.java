/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.actuate.endpoint.jmx;

import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

/**
 * A factory to create an {@link ObjectName} for an {@link EndpointMBean}.
 *
 * @author Stephane Nicoll
 * @since 2.0.0
 */
@FunctionalInterface
public interface EndpointObjectNameFactory {

	/**
	 * Generate an {@link ObjectName} for the specified {@link ExposableJmxEndpoint
	 * endpoint}.
	 * @param endpoint the endpoint MBean to handle
	 * @return the {@link ObjectName} to use for the endpoint
	 * @throws MalformedObjectNameException if the object name is invalid
	 */
	ObjectName getObjectName(ExposableJmxEndpoint endpoint) throws MalformedObjectNameException;

}
