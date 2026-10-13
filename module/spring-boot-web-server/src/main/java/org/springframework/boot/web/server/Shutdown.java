/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.web.server;

/**
 * Configuration for shutting down a {@link WebServer}.
 *
 * @author Andy Wilkinson
 * @since 2.3.0
 */
public enum Shutdown {

	/**
	 * The {@link WebServer} should support graceful shutdown, allowing active requests
	 * time to complete.
	 */
	GRACEFUL,

	/**
	 * The {@link WebServer} should shut down immediately.
	 */
	IMMEDIATE

}
