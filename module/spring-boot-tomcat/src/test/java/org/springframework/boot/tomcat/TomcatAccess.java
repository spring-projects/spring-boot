/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.tomcat;

import java.util.Map;

import org.apache.catalina.Service;
import org.apache.catalina.connector.Connector;

/**
 * Helper class to provide public access to package-private methods for testing purposes.
 *
 * @author Andy Wilkinson
 */
public final class TomcatAccess {

	private TomcatAccess() {

	}

	public static Map<Service, Connector[]> getServiceConnectors(TomcatWebServer tomcatWebServer) {
		return tomcatWebServer.getServiceConnectors();
	}

	public static String getStartedLogMessage(TomcatWebServer tomcatWebServer) {
		return tomcatWebServer.getStartedLogMessage();
	}

}
