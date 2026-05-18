/*
 * Copyright 2012-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.opentelemetry.autoconfigure;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

import org.jspecify.annotations.Nullable;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Common configuration properties for OpenTelemetry Protocol (OTLP) exporters.
 *
 * @author Somil Jain
 * @since 4.0.0
 */
@ConfigurationProperties("management.opentelemetry.otlp")
public class OtlpProperties {

	/**
	 * OTLP endpoint to connect to.
	 */
	private @Nullable String endpoint;

	/**
	 * Additional headers to be passed with every request.
	 */
	private final Map<String, String> headers = new LinkedHashMap<>();

	/**
	 * Timeout for executing requests.
	 */
	private @Nullable Duration timeout;

	/**
	 * Connection timeout.
	 */
	private @Nullable Duration connectTimeout;

	/**
	 * Transport to use.
	 */
	private @Nullable Transport transport;

	/**
	 * Compression to use.
	 */
	private @Nullable Compression compression;

	public @Nullable String getEndpoint() {
		return this.endpoint;
	}

	public void setEndpoint(@Nullable String endpoint) {
		this.endpoint = endpoint;
	}

	public Map<String, String> getHeaders() {
		return this.headers;
	}

	public @Nullable Duration getTimeout() {
		return this.timeout;
	}

	public void setTimeout(@Nullable Duration timeout) {
		this.timeout = timeout;
	}

	public @Nullable Duration getConnectTimeout() {
		return this.connectTimeout;
	}

	public void setConnectTimeout(@Nullable Duration connectTimeout) {
		this.connectTimeout = connectTimeout;
	}

	public @Nullable Transport getTransport() {
		return this.transport;
	}

	public void setTransport(@Nullable Transport transport) {
		this.transport = transport;
	}

	public @Nullable Compression getCompression() {
		return this.compression;
	}

	public void setCompression(@Nullable Compression compression) {
		this.compression = compression;
	}

	public enum Transport {

		/**
		 * HTTP transport.
		 */
		HTTP,

		/**
		 * gRPC transport.
		 */
		GRPC

	}

	public enum Compression {

		/**
		 * No compression.
		 */
		NONE,

		/**
		 * GZIP compression.
		 */
		GZIP

	}

}
