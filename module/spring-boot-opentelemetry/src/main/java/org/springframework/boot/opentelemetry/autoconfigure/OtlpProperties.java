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

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;

import org.jspecify.annotations.Nullable;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;

/**
 * Common configuration properties for OpenTelemetry Protocol (OTLP) exporters.
 *
 * @author Somil Jain
 * @since 4.2.0
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
	 * Method used to compress the payload.
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

	public @Nullable Compression getCompression() {
		return this.compression;
	}

	public void setCompression(@Nullable Compression compression) {
		this.compression = compression;
	}

	/**
	 * Resolves the endpoint to use, falling back to this common endpoint (with
	 * {@code path} appended) when {@code signalEndpoint} is not set.
	 * @param signalEndpoint the signal-specific endpoint, or {@code null} if not set
	 * @param path the path to append to the common endpoint when used as a fallback, or
	 * {@code null} if no path should be appended
	 * @return the resolved endpoint, or {@code null} if neither endpoint is set
	 */
	public @Nullable String resolveEndpoint(@Nullable String signalEndpoint, @Nullable String path) {
		if (StringUtils.hasLength(signalEndpoint)) {
			return signalEndpoint;
		}
		if (this.endpoint == null || path == null) {
			return this.endpoint;
		}
		return this.endpoint.endsWith("/") ? this.endpoint + path : this.endpoint + "/" + path;
	}

	/**
	 * Merges the given signal-specific headers with these common headers. Entries in
	 * {@code signalHeaders} take precedence over common headers with the same key.
	 * @param signalHeaders the signal-specific headers
	 * @return the merged headers
	 */
	public Map<String, String> mergeHeaders(Map<String, String> signalHeaders) {
		Map<String, String> merged = new LinkedHashMap<>(this.headers);
		merged.putAll(signalHeaders);
		return merged;
	}

	/**
	 * Resolves the compression to use, falling back to this common compression, mapped
	 * through {@code mapper}, when {@code signalCompression} is not set.
	 * @param <T> the signal-specific compression type
	 * @param signalCompression the signal-specific compression, or {@code null} if not
	 * set
	 * @param defaultCompression the compression to use when neither is set
	 * @param mapper maps this common compression to the signal-specific type
	 * @return the resolved compression
	 */
	public <T> T resolveCompression(@Nullable T signalCompression, T defaultCompression,
			Function<Compression, T> mapper) {
		if (signalCompression != null) {
			return signalCompression;
		}
		if (this.compression != null) {
			return mapper.apply(this.compression);
		}
		return defaultCompression;
	}

	/**
	 * Compression methods.
	 */
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
