/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.actuate.endpoint.web;

import java.util.List;

import org.jspecify.annotations.Nullable;

import org.springframework.boot.actuate.endpoint.EndpointId;

/**
 * Strategy interface used to provide a mapping between an endpoint ID and any additional
 * paths where it will be exposed.
 *
 * @author Phillip Webb
 * @since 3.4.0
 */
@FunctionalInterface
public interface AdditionalPathsMapper {

	/**
	 * Resolve the additional paths for the specified {@code endpointId} and web server
	 * namespace.
	 * @param endpointId the id of an endpoint
	 * @param webServerNamespace the web server namespace
	 * @return the additional paths of the endpoint or {@code null} if this mapper doesn't
	 * support the given endpoint ID.
	 */
	@Nullable List<String> getAdditionalPaths(EndpointId endpointId, WebServerNamespace webServerNamespace);

}
