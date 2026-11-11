/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.http.client.autoconfigure;

import java.time.Duration;

import org.jspecify.annotations.Nullable;

import org.springframework.boot.http.client.HttpClientSettings;
import org.springframework.boot.http.client.HttpCookieHandling;
import org.springframework.boot.http.client.HttpRedirects;

/**
 * Base class for configuration properties configure {@link HttpClientSettings}.
 *
 * @author Phillip Webb
 * @since 4.0.0
 * @see HttpClientSettings
 */
public abstract class HttpClientSettingsProperties {

	/**
	 * Handling for HTTP redirects.
	 */
	private @Nullable HttpRedirects redirects;

	/**
	 * Default connect timeout for a client HTTP request.
	 */
	private @Nullable Duration connectTimeout;

	/**
	 * Default read timeout for a client HTTP request.
	 */
	private @Nullable Duration readTimeout;

	/**
	 * Handling for HTTP cookies.
	 */
	private @Nullable HttpCookieHandling cookieHandling;

	/**
	 * Default SSL configuration for a client HTTP request.
	 */
	private final Ssl ssl = new Ssl();

	public @Nullable HttpRedirects getRedirects() {
		return this.redirects;
	}

	public void setRedirects(@Nullable HttpRedirects redirects) {
		this.redirects = redirects;
	}

	public @Nullable Duration getConnectTimeout() {
		return this.connectTimeout;
	}

	public void setConnectTimeout(@Nullable Duration connectTimeout) {
		this.connectTimeout = connectTimeout;
	}

	public @Nullable Duration getReadTimeout() {
		return this.readTimeout;
	}

	public void setReadTimeout(@Nullable Duration readTimeout) {
		this.readTimeout = readTimeout;
	}

	public @Nullable HttpCookieHandling getCookieHandling() {
		return this.cookieHandling;
	}

	public void setCookieHandling(@Nullable HttpCookieHandling cookieHandling) {
		this.cookieHandling = cookieHandling;
	}

	public Ssl getSsl() {
		return this.ssl;
	}

	/**
	 * SSL configuration.
	 */
	public static class Ssl {

		/**
		 * SSL bundle to use.
		 */
		private @Nullable String bundle;

		public @Nullable String getBundle() {
			return this.bundle;
		}

		public void setBundle(@Nullable String bundle) {
			this.bundle = bundle;
		}

	}

}
