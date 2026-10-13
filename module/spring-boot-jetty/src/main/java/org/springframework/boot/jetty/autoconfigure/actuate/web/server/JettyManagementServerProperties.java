/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.jetty.autoconfigure.actuate.web.server;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Properties for a Jetty-based management server.
 *
 * @author Moritz Halbritter
 * @since 4.0.0
 */
@ConfigurationProperties("management.server.jetty")
public class JettyManagementServerProperties {

	private final Accesslog accesslog = new Accesslog();

	public Accesslog getAccesslog() {
		return this.accesslog;
	}

	public static class Accesslog {

		/**
		 * Management log file name prefix.
		 */
		private String prefix = "management_";

		public String getPrefix() {
			return this.prefix;
		}

		public void setPrefix(String prefix) {
			this.prefix = prefix;
		}

	}

}
