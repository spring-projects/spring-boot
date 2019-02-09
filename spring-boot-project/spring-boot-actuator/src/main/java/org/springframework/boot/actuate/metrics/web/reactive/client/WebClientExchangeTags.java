/*
 * Copyright 2012-2019 the original author or authors.
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

package org.springframework.boot.actuate.metrics.web.reactive.client;

import java.io.IOException;
import java.util.regex.Pattern;

import io.micrometer.core.instrument.Tag;

import org.springframework.http.HttpStatus;
import org.springframework.http.client.reactive.ClientHttpRequest;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Factory methods for creating {@link Tag Tags} related to a request-response exchange
 * performed by a {@link WebClient}.
 *
 * @author Brian Clozel
 * @author Nishant Raut
 * @since 2.1.0
 */
public final class WebClientExchangeTags {

	private static final String URI_TEMPLATE_ATTRIBUTE = WebClient.class.getName()
			+ ".uriTemplate";

	private static final Tag IO_ERROR = Tag.of("status", "IO_ERROR");

	private static final Tag CLIENT_ERROR = Tag.of("status", "CLIENT_ERROR");

	private static final Pattern PATTERN_BEFORE_PATH = Pattern
			.compile("^https?://[^/]+/");

	private static final Tag CLIENT_NAME_NONE = Tag.of("clientName", "none");

	private static final Tag OUTCOME_UNKNOWN = Tag.of("outcome", "UNKNOWN");

	private static final Tag OUTCOME_INFORMATIONAL = Tag.of("outcome", "INFORMATIONAL");

	private static final Tag OUTCOME_SUCCESS = Tag.of("outcome", "SUCCESS");

	private static final Tag OUTCOME_REDIRECTION = Tag.of("outcome", "REDIRECTION");

	private static final Tag OUTCOME_CLIENT_ERROR = Tag.of("outcome", "CLIENT_ERROR");

	private static final Tag OUTCOME_SERVER_ERROR = Tag.of("outcome", "SERVER_ERROR");

	private WebClientExchangeTags() {
	}

	/**
	 * Creates a {@code method} {@code Tag} for the {@link ClientHttpRequest#getMethod()
	 * method} of the given {@code request}.
	 * @param request the request
	 * @return the method tag
	 */
	public static Tag method(ClientRequest request) {
		return Tag.of("method", request.method().name());
	}

	/**
	 * Creates a {@code uri} {@code Tag} for the URI path of the given {@code request}.
	 * @param request the request
	 * @return the uri tag
	 */
	public static Tag uri(ClientRequest request) {
		String uri = (String) request.attribute(URI_TEMPLATE_ATTRIBUTE)
				.orElseGet(() -> request.url().getPath());
		return Tag.of("uri", extractPath(uri));
	}

	private static String extractPath(String url) {
		String path = PATTERN_BEFORE_PATH.matcher(url).replaceFirst("");
		return (path.startsWith("/") ? path : "/" + path);
	}

	/**
	 * Creates a {@code status} {@code Tag} derived from the
	 * {@link ClientResponse#statusCode()} of the given {@code response}.
	 * @param response the response
	 * @return the status tag
	 */
	public static Tag status(ClientResponse response) {
		return Tag.of("status", String.valueOf(response.statusCode().value()));
	}

	/**
	 * Creates a {@code status} {@code Tag} derived from the exception thrown by the
	 * client.
	 * @param throwable the exception
	 * @return the status tag
	 */
	public static Tag status(Throwable throwable) {
		return (throwable instanceof IOException) ? IO_ERROR : CLIENT_ERROR;
	}

	/**
	 * Create a {@code clientName} {@code Tag} derived from the
	 * {@link java.net.URI#getHost host} of the {@link ClientRequest#url() URL} of the
	 * given {@code request}.
	 * @param request the request
	 * @return the clientName tag
	 */
	public static Tag clientName(ClientRequest request) {
		String host = request.url().getHost();
		if (host == null) {
			return CLIENT_NAME_NONE;
		}
		return Tag.of("clientName", host);
	}

	/**
	 * Creates a {@code outcome} {@code Tag} derived from the
	 * {@link ClientResponse#statusCode() status} of the given {@code response}.
	 * @param response the response
	 * @return the outcome tag
	 * @since 2.2.0
	 */
	public static Tag outcome(ClientResponse response) {
		try {
			if (response != null) {
				HttpStatus status = response.statusCode();
				if (status.is1xxInformational()) {
					return OUTCOME_INFORMATIONAL;
				}
				if (status.is2xxSuccessful()) {
					return OUTCOME_SUCCESS;
				}
				if (status.is3xxRedirection()) {
					return OUTCOME_REDIRECTION;
				}
				if (status.is4xxClientError()) {
					return OUTCOME_CLIENT_ERROR;
				}
				if (status.is5xxServerError()) {
					return OUTCOME_SERVER_ERROR;
				}
			}
			return OUTCOME_UNKNOWN;
		}
		catch (IllegalArgumentException exc) {
			return OUTCOME_UNKNOWN;
		}
	}

}
