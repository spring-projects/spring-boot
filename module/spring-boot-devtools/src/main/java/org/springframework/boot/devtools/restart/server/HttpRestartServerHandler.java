/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.devtools.restart.server;

import java.io.IOException;

import org.springframework.boot.devtools.remote.server.Handler;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.util.Assert;

/**
 * Adapts {@link HttpRestartServer} to a {@link Handler}.
 *
 * @author Phillip Webb
 * @since 1.3.0
 */
public class HttpRestartServerHandler implements Handler {

	private final HttpRestartServer server;

	/**
	 * Create a new {@link HttpRestartServerHandler} instance.
	 * @param server the server to adapt
	 */
	public HttpRestartServerHandler(HttpRestartServer server) {
		Assert.notNull(server, "'server' must not be null");
		this.server = server;
	}

	@Override
	public void handle(ServerHttpRequest request, ServerHttpResponse response) throws IOException {
		this.server.handle(request, response);
	}

}
