/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.devtools.remote.server;

import java.io.IOException;

import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.util.Assert;

/**
 * {@link Handler} that responds with a specific {@link HttpStatus}.
 *
 * @author Phillip Webb
 * @since 1.3.0
 */
public class HttpStatusHandler implements Handler {

	private final HttpStatus status;

	/**
	 * Create a new {@link HttpStatusHandler} instance that will respond with an HTTP OK
	 * 200 status.
	 */
	public HttpStatusHandler() {
		this(HttpStatus.OK);
	}

	/**
	 * Create a new {@link HttpStatusHandler} instance that will respond with the
	 * specified status.
	 * @param status the status
	 */
	public HttpStatusHandler(HttpStatus status) {
		Assert.notNull(status, "'status' must not be null");
		this.status = status;
	}

	@Override
	public void handle(ServerHttpRequest request, ServerHttpResponse response) throws IOException {
		response.setStatusCode(this.status);
	}

}
