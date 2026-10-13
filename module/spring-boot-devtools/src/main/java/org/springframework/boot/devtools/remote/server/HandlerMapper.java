/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.devtools.remote.server;

import org.jspecify.annotations.Nullable;

import org.springframework.http.server.ServerHttpRequest;

/**
 * Interface to provide a mapping between a {@link ServerHttpRequest} and a
 * {@link Handler}.
 *
 * @author Phillip Webb
 * @since 1.3.0
 */
@FunctionalInterface
public interface HandlerMapper {

	/**
	 * Return the handler for the given request or {@code null}.
	 * @param request the request
	 * @return a {@link Handler} or {@code null}
	 */
	@Nullable Handler getHandler(ServerHttpRequest request);

}
