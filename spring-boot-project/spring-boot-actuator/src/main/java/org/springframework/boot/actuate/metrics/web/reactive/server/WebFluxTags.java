/*
 * Copyright 2012-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.actuate.metrics.web.reactive.server;

import io.micrometer.core.instrument.Tag;

import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.HandlerMapping;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.util.pattern.PathPattern;

/**
 * Factory methods for {@link Tag Tags} associated with a request-response exchange that
 * is handled by WebFlux.
 *
 * @author Jon Schneider
 * @author Andy Wilkinson
 * @since 2.0.0
 */
public final class WebFluxTags {

	private static final Tag URI_NOT_FOUND = Tag.of("uri", "NOT_FOUND");

	private static final Tag URI_REDIRECTION = Tag.of("uri", "REDIRECTION");

	private WebFluxTags() {
	}

	/**
	 * Creates a {@code method} tag based on the
	 * {@link org.springframework.http.server.reactive.ServerHttpRequest#getMethod()
	 * method} of the {@link ServerWebExchange#getRequest()} request of the given
	 * {@code exchange}.
	 * @param exchange the exchange
	 * @return the method tag whose value is a capitalized method (e.g. GET).
	 */
	public static Tag method(ServerWebExchange exchange) {
		return Tag.of("method", exchange.getRequest().getMethod().toString());
	}

	/**
	 * Creates a {@code method} tag based on the response status of the given
	 * {@code exchange}.
	 * @param exchange the exchange
	 * @return the "status" tag derived from the response status
	 */
	public static Tag status(ServerWebExchange exchange) {
		HttpStatus status = exchange.getResponse().getStatusCode();
		if (status == null) {
			status = HttpStatus.OK;
		}
		return Tag.of("status", status.toString());
	}

	/**
	 * Creates a {@code uri} tag based on the URI of the given {@code exchange}. Uses the
	 * {@link HandlerMapping#BEST_MATCHING_PATTERN_ATTRIBUTE} best matching pattern.
	 * @param exchange the exchange
	 * @return the uri tag derived from the exchange
	 */
	public static Tag uri(ServerWebExchange exchange) {
		if (exchange != null) {
			PathPattern pathPattern = exchange
					.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
			if (pathPattern != null) {
				return Tag.of("uri", pathPattern.getPatternString());
			}
			HttpStatus status = exchange.getResponse().getStatusCode();
			if (status != null && status.is3xxRedirection()) {
				return URI_REDIRECTION;
			}
			if (status != null && status.equals(HttpStatus.NOT_FOUND)) {
				return URI_NOT_FOUND;
			}
			String path = exchange.getRequest().getPath().value();
			return Tag.of("uri", path.isEmpty() ? "root" : path);
		}
		return Tag.of("uri", "UNKNOWN");
	}

	/**
	 * Creates an {@code exception} tag based on the {@link Class#getSimpleName() simple
	 * name} of the class of the given {@code exception}.
	 * @param exception the exception, may be {@code null}
	 * @return the exception tag derived from the exception
	 */
	public static Tag exception(Throwable exception) {
		if (exception != null) {
			return Tag.of("exception", exception.getClass().getSimpleName());
		}
		return Tag.of("exception", "none");
	}

}
