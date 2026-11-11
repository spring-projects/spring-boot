/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.micrometer.tracing.opentelemetry.autoconfigure.otlp;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

import org.jspecify.annotations.Nullable;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for exporting traces using OTLP.
 *
 * @author Jonatan Ivanov
 * @since 4.0.0
 */
@ConfigurationProperties("management.opentelemetry.tracing.export.otlp")
public class OtlpTracingProperties {

	/**
	 * URL to the OTel collector's HTTP API.
	 */
	private @Nullable String endpoint;

	/**
	 * Call timeout for the OTel Collector to process an exported batch of data. This
	 * timeout spans the entire call: resolving DNS, connecting, writing the request body,
	 * server processing, and reading the response body. If the call requires redirects or
	 * retries all must complete within one timeout period.
	 */
	private Duration timeout = Duration.ofSeconds(10);

	/**
	 * Connect timeout for the OTel collector connection.
	 */
	private Duration connectTimeout = Duration.ofSeconds(10);

	/**
	 * Transport used to send the spans.
	 */
	private Transport transport = Transport.HTTP;

	/**
	 * Method used to compress the payload.
	 */
	private @Nullable Compression compression;

	/**
	 * Custom HTTP headers you want to pass to the collector, for example auth headers.
	 */
	private Map<String, String> headers = new LinkedHashMap<>();

	private final Ssl ssl = new Ssl();

	public @Nullable String getEndpoint() {
		return this.endpoint;
	}

	public void setEndpoint(@Nullable String endpoint) {
		this.endpoint = endpoint;
	}

	public Duration getTimeout() {
		return this.timeout;
	}

	public void setTimeout(Duration timeout) {
		this.timeout = timeout;
	}

	public Duration getConnectTimeout() {
		return this.connectTimeout;
	}

	public void setConnectTimeout(Duration connectTimeout) {
		this.connectTimeout = connectTimeout;
	}

	public Transport getTransport() {
		return this.transport;
	}

	public void setTransport(Transport transport) {
		this.transport = transport;
	}

	public @Nullable Compression getCompression() {
		return this.compression;
	}

	public void setCompression(@Nullable Compression compression) {
		this.compression = compression;
	}

	public Map<String, String> getHeaders() {
		return this.headers;
	}

	public void setHeaders(Map<String, String> headers) {
		this.headers = headers;
	}

	public Ssl getSsl() {
		return this.ssl;
	}

	public static class Ssl {

		/**
		 * SSL bundle name.
		 */
		private @Nullable String bundle;

		public @Nullable String getBundle() {
			return this.bundle;
		}

		public void setBundle(@Nullable String bundle) {
			this.bundle = bundle;
		}

	}

	public enum Compression {

		/**
		 * Gzip compression.
		 */
		GZIP,

		/**
		 * No compression.
		 */
		NONE

	}

}
