/*
 * Copyright 2012-2023 the original author or authors.
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

package org.springframework.boot.actuate.autoconfigure.tracing.otlp;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for exporting traces using OTLP.
 *
 * @author Jonatan Ivanov
 * @since 3.1.0
 */
@ConfigurationProperties("management.otlp.tracing")
public class OtlpProperties {

	/**
	 * URL to the OTel collector's HTTP API.
	 */
	private String endpoint;

	/**
	 * Call timeout for the OTel Collector to process an exported batch of data. This
	 * timeout spans the entire call: resolving DNS, connecting, writing the request body,
	 * server processing, and reading the response body. If the call requires redirects or
	 * retries all must complete within one timeout period.
	 */
	private Duration timeout = Duration.ofSeconds(10);

	/**
	 * Method used to compress the payload.
	 */
	private Compression compression = Compression.NONE;

	/**
	 * Custom HTTP headers you want to pass to the collector, for example auth headers.
	 */
	private Map<String, String> headers = new HashMap<>();

	public String getEndpoint() {
		return this.endpoint;
	}

	public void setEndpoint(String endpoint) {
		this.endpoint = endpoint;
	}

	public Duration getTimeout() {
		return this.timeout;
	}

	public void setTimeout(Duration timeout) {
		this.timeout = timeout;
	}

	public Compression getCompression() {
		return this.compression;
	}

	public void setCompression(Compression compression) {
		this.compression = compression;
	}

	public Map<String, String> getHeaders() {
		return this.headers;
	}

	public void setHeaders(Map<String, String> headers) {
		this.headers = headers;
	}

	enum Compression {

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
