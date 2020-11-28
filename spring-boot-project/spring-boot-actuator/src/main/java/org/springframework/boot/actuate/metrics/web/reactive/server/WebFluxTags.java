/*
 * Copyright 2012-2020 the original author or authors.
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

package org.springframework.boot.actuate.metrics.web.reactive.server;

import java.util.regex.Pattern;

import io.micrometer.core.instrument.Tag;

import org.springframework.boot.actuate.metrics.http.Outcome;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.HandlerMapping;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.util.pattern.PathPattern;

/**
 * Factory methods for {@link Tag Tags} associated with a request-response exchange that
 * is handled by WebFlux.
 *
 * @author Jon Schneider
 * @author Andy Wilkinson
 * @author Michael McFadyen
 * @since 2.0.0
 */
public final class WebFluxTags {

	private static final Tag URI_NOT_FOUND = Tag.of("uri", "NOT_FOUND");

	private static final Tag URI_REDIRECTION = Tag.of("uri", "REDIRECTION");

	private static final Tag URI_ROOT = Tag.of("uri", "root");

	private static final Tag URI_UNKNOWN = Tag.of("uri", "UNKNOWN");

	private static final Tag EXCEPTION_NONE = Tag.of("exception", "None");

	private static final Pattern FORWARD_SLASHES_PATTERN = Pattern.compile("//+");

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
		return Tag.of("method", exchange.getRequest().getMethodValue());
	}

	/**
	 * Creates a {@code status} tag based on the response status of the given
	 * {@code exchange}.
	 * @param exchange the exchange
	 * @return the status tag derived from the response status
	 */
	public static Tag status(ServerWebExchange exchange) {
		HttpStatus status = exchange.getResponse().getStatusCode();
		if (status == null) {
			status = HttpStatus.OK;
		}
		return Tag.of("status", String.valueOf(status.value()));
	}

	/**
	 * Creates a {@code uri} tag based on the URI of the given {@code exchange}. Uses the
	 * {@link HandlerMapping#BEST_MATCHING_PATTERN_ATTRIBUTE} best matching pattern if
	 * available. Falling back to {@code REDIRECTION} for 3xx responses, {@code NOT_FOUND}
	 * for 404 responses, {@code root} for requests with no path info, and {@code UNKNOWN}
	 * for all other requests.
	 * @param exchange the exchange
	 * @return the uri tag derived from the exchange
	 */
	public static Tag uri(ServerWebExchange exchange) {
		return uri(exchange, false);
	}

	/**
	 * Creates a {@code uri} tag based on the URI of the given {@code exchange}. Uses the
	 * {@link HandlerMapping#BEST_MATCHING_PATTERN_ATTRIBUTE} best matching pattern if
	 * available. Falling back to {@code REDIRECTION} for 3xx responses, {@code NOT_FOUND}
	 * for 404 responses, {@code root} for requests with no path info, and {@code UNKNOWN}
	 * for all other requests.
	 * @param exchange the exchange
	 * @param ignoreTrailingSlash whether to ignore the trailing slash
	 * @return the uri tag derived from the exchange
	 */
	public static Tag uri(ServerWebExchange exchange, boolean ignoreTrailingSlash) {
		PathPattern pathPattern = exchange.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
		if (pathPattern != null) {
			String patternString = pathPattern.getPatternString();
			if (ignoreTrailingSlash && patternString.length() > 1) {
				patternString = removeTrailingSlash(patternString);
			}
			if (patternString.isEmpty()) {
				return URI_ROOT;
			}
			return Tag.of("uri", patternString);
		}
		HttpStatus status = exchange.getResponse().getStatusCode();
		if (status != null) {
			if (status.is3xxRedirection()) {
				return URI_REDIRECTION;
			}
			if (status == HttpStatus.NOT_FOUND) {
				return URI_NOT_FOUND;
			}
		}
		String path = getPathInfo(exchange);
		if (path.isEmpty()) {
			return URI_ROOT;
		}
		return URI_UNKNOWN;
	}

	private static String getPathInfo(ServerWebExchange exchange) {
		String path = exchange.getRequest().getPath().value();
		String uri = StringUtils.hasText(path) ? path : "/";
		String singleSlashes = FORWARD_SLASHES_PATTERN.matcher(uri).replaceAll("/");
		return removeTrailingSlash(singleSlashes);
	}

	private static String removeTrailingSlash(String text) {
		if (!StringUtils.hasLength(text)) {
			return text;
		}
		return text.endsWith("/") ? text.substring(0, text.length() - 1) : text;
	}

	/**
	 * Creates an {@code exception} tag based on the {@link Class#getSimpleName() simple
	 * name} of the class of the given {@code exception}.
	 * @param exception the exception, may be {@code null}
	 * @return the exception tag derived from the exception
	 */
	public static Tag exception(Throwable exception) {
		if (exception != null) {
			String simpleName = exception.getClass().getSimpleName();
			return Tag.of("exception", StringUtils.hasText(simpleName) ? simpleName : exception.getClass().getName());
		}
		return EXCEPTION_NONE;
	}

	/**
	 * Creates an {@code outcome} tag based on the response status of the given
	 * {@code exchange}.
	 * @param exchange the exchange
	 * @return the outcome tag derived from the response status
	 * @since 2.1.0
	 */
	public static Tag outcome(ServerWebExchange exchange) {
		Integer statusCode = extractStatusCode(exchange);
		Outcome outcome = (statusCode != null) ? Outcome.forStatus(statusCode) : Outcome.SUCCESS;
		return outcome.asTag();
	}

	private static Integer extractStatusCode(ServerWebExchange exchange) {
		ServerHttpResponse response = exchange.getResponse();
		Integer statusCode = response.getRawStatusCode();
		if (statusCode != null) {
			return statusCode;
		}
		HttpStatus status = response.getStatusCode();
		return (status != null) ? status.value() : null;
	}

}
