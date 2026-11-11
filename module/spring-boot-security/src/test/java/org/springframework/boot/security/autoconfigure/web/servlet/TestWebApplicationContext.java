/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.security.autoconfigure.web.servlet;

import org.jspecify.annotations.Nullable;

import org.springframework.boot.web.server.WebServer;
import org.springframework.boot.web.server.context.WebServerApplicationContext;
import org.springframework.web.context.support.StaticWebApplicationContext;

/**
 * Test {@link StaticWebApplicationContext} that also implements
 * {@link WebServerApplicationContext}.
 *
 * @author Phillip Webb
 */
class TestWebApplicationContext extends StaticWebApplicationContext implements WebServerApplicationContext {

	private final @Nullable String serverNamespace;

	TestWebApplicationContext(@Nullable String serverNamespace) {
		this.serverNamespace = serverNamespace;
	}

	@Override
	public @Nullable WebServer getWebServer() {
		return null;
	}

	@Override
	public @Nullable String getServerNamespace() {
		return this.serverNamespace;
	}

}
