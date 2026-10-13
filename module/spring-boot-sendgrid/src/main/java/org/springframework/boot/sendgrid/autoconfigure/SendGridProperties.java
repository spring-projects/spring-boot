/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.sendgrid.autoconfigure;

import org.jspecify.annotations.Nullable;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * {@link ConfigurationProperties @ConfigurationProperties} for SendGrid.
 *
 * @author Maciej Walkowiak
 * @author Andy Wilkinson
 * @since 4.0.0
 */
@ConfigurationProperties("spring.sendgrid")
public class SendGridProperties {

	/**
	 * SendGrid API key.
	 */
	private @Nullable String apiKey;

	/**
	 * Proxy configuration.
	 */
	private @Nullable Proxy proxy;

	public @Nullable String getApiKey() {
		return this.apiKey;
	}

	public void setApiKey(@Nullable String apiKey) {
		this.apiKey = apiKey;
	}

	public @Nullable Proxy getProxy() {
		return this.proxy;
	}

	public void setProxy(@Nullable Proxy proxy) {
		this.proxy = proxy;
	}

	public static class Proxy {

		/**
		 * SendGrid proxy host.
		 */
		private @Nullable String host;

		/**
		 * SendGrid proxy port.
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
