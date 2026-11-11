/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.actuate.endpoint.web;

import java.util.Collections;
import java.util.List;

import org.springframework.boot.actuate.endpoint.ExposableEndpoint;

/**
 * Interface that can be implemented by an {@link ExposableEndpoint} that is mapped to a
 * root web path.
 *
 * @author Phillip Webb
 * @since 2.0.0
 * @see PathMapper
 */
@FunctionalInterface
public interface PathMappedEndpoint {

	/**
	 * Return the root path of the endpoint (relative to the context and base path) that
	 * exposes it. For example, a root path of {@code example} would be exposed under the
	 * URL "/{actuator-context}/example".
	 * @return the root path for the endpoint
	 * @see PathMappedEndpoints#getBasePath
	 */
	String getRootPath();

	/**
	 * Return any additional paths (relative to the context) for the given
	 * {@link WebServerNamespace}.
	 * @param webServerNamespace the web server namespace
	 * @return a list of additional paths
	 * @since 3.4.0
	 */
	default List<String> getAdditionalPaths(WebServerNamespace webServerNamespace) {
		return Collections.emptyList();
	}

}
