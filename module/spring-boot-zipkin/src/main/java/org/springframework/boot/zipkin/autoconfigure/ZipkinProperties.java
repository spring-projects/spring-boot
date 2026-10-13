/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.zipkin.autoconfigure;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for {@link ZipkinAutoConfiguration}.
 *
 * @author Moritz Halbritter
 * @since 4.0.0
 */
@ConfigurationProperties("management.tracing.export.zipkin")
public class ZipkinProperties {

	/**
	 * URL to the Zipkin API.
	 */
	private String endpoint = "http://localhost:9411/api/v2/spans";

	/**
	 * How to encode the POST body to the Zipkin API.
	 */
	private Encoding encoding = Encoding.JSON;

	/**
	 * Connection timeout for requests to Zipkin.
	 */
	private Duration connectTimeout = Duration.ofSeconds(1);

	/**
	 * Read timeout for requests to Zipkin.
	 */
	private Duration readTimeout = Duration.ofSeconds(10);

	public String getEndpoint() {
		return this.endpoint;
	}

	public void setEndpoint(String endpoint) {
		this.endpoint = endpoint;
	}

	public Encoding getEncoding() {
		return this.encoding;
	}

	public void setEncoding(Encoding encoding) {
		this.encoding = encoding;
	}

	public Duration getConnectTimeout() {
		return this.connectTimeout;
	}

	public void setConnectTimeout(Duration connectTimeout) {
		this.connectTimeout = connectTimeout;
	}

	public Duration getReadTimeout() {
		return this.readTimeout;
	}

	public void setReadTimeout(Duration readTimeout) {
		this.readTimeout = readTimeout;
	}

	/**
	 * Zipkin message encoding.
	 */
	public enum Encoding {

		/**
		 * JSON.
		 */
		JSON,

		/**
		 * Protocol Buffers v3.
		 */
		PROTO3

	}

}
