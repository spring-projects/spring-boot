/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.jetty;

/**
 * Helper class to provide public access to package-private methods for testing purposes.
 *
 * @author Andy Wilkinson
 */
public final class JettyAccess {

	private JettyAccess() {

	}

	public static String getStartedLogMessage(JettyWebServer jettyWebServer) {
		return jettyWebServer.getStartedLogMessage();
	}

}
