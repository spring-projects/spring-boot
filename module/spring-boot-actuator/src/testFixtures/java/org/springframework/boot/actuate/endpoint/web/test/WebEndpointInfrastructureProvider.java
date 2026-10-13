/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.actuate.endpoint.web.test;

import java.util.List;

import org.springframework.boot.actuate.endpoint.web.test.WebEndpointTest.Infrastructure;

/**
 * Strategy interface to provide the web endpoint configuration for a target
 * {@linkplain Infrastructure infrastructure}.
 *
 * @author Stephane Nicoll
 * @since 4.0.0
 */
public interface WebEndpointInfrastructureProvider {

	/**
	 * Specify if the given {@link Infrastructure} is supported.
	 * @param infrastructure the infrastructure to target
	 * @return {@code true} if this instance can provide the configuration
	 */
	boolean supports(Infrastructure infrastructure);

	/**
	 * Return the configuration to use for the given {@code infrastructure}.
	 * @param infrastructure the infrastructure to target
	 * @return the configuration for that infrastructure
	 */
	List<Class<?>> getInfrastructureConfiguration(Infrastructure infrastructure);

}
