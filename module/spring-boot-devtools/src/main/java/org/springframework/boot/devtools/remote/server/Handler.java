/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.devtools.remote.server;

import java.io.IOException;

import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;

/**
 * A single handler that is able to process an incoming remote server request.
 *
 * @author Phillip Webb
 * @since 1.3.0
 */
@FunctionalInterface
public interface Handler {

	/**
	 * Handle the request.
	 * @param request the request
	 * @param response the response
	 * @throws IOException in case of I/O errors
	 */
	void handle(ServerHttpRequest request, ServerHttpResponse response) throws IOException;

}
