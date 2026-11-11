/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.devtools.autoconfigure;

import org.jspecify.annotations.Nullable;

/**
 * Configuration properties for remote Spring Boot applications.
 *
 * @author Phillip Webb
 * @author Rob Winch
 * @since 1.3.0
 * @see DevToolsProperties
 */
public class RemoteDevToolsProperties {

	public static final String DEFAULT_CONTEXT_PATH = "/.~~spring-boot!~";

	public static final String DEFAULT_SECRET_HEADER_NAME = "X-AUTH-TOKEN";

	/**
	 * Context path used to handle the remote connection.
	 */
	private String contextPath = DEFAULT_CONTEXT_PATH;

	/**
	 * A shared secret required to establish a connection (required to enable remote
	 * support).
	 */
	private @Nullable String secret;

	/**
	 * HTTP header used to transfer the shared secret.
	 */
	private String secretHeaderName = DEFAULT_SECRET_HEADER_NAME;

	private final Restart restart = new Restart();

	private final Proxy proxy = new Proxy();

	public String getContextPath() {
		return this.contextPath;
	}

	public void setContextPath(String contextPath) {
		this.contextPath = contextPath;
	}

	public @Nullable String getSecret() {
		return this.secret;
	}

	public void setSecret(@Nullable String secret) {
		this.secret = secret;
	}

	public String getSecretHeaderName() {
		return this.secretHeaderName;
	}

	public void setSecretHeaderName(String secretHeaderName) {
		this.secretHeaderName = secretHeaderName;
	}

	public Restart getRestart() {
		return this.restart;
	}

	public Proxy getProxy() {
		return this.proxy;
	}

	public static class Restart {

		/**
		 * Whether to enable remote restart.
		 */
		private boolean enabled = true;

		public boolean isEnabled() {
			return this.enabled;
		}

		public void setEnabled(boolean enabled) {
			this.enabled = enabled;
		}

	}

	public static class Proxy {

		/**
		 * The host of the proxy to use to connect to the remote application.
		 */
		private @Nullable String host;

		/**
		 * The port of the proxy to use to connect to the remote application.
		 */
		private @Nullable Integer port;

		public @Nullable String getHost() {
			return this.host;
		}

		public void setHost(@Nullable String host) {
			this.host = host;
		}

		public @Nullable Integer getPort() {
			return this.port;
		}

		public void setPort(@Nullable Integer port) {
			this.port = port;
		}

	}

}
