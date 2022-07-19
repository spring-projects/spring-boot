/*
 * Copyright 2012-2022 the original author or authors.
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

package org.springframework.boot.actuate.metrics.web.client;

import java.io.IOException;
import java.net.URI;
import java.util.regex.Pattern;

import io.micrometer.core.instrument.Tag;

import org.springframework.boot.actuate.metrics.http.Outcome;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

/**
 * Factory methods for creating {@link Tag Tags} related to a request-response exchange
 * performed by a {@link RestTemplate}.
 *
 * @author Andy Wilkinson
 * @author Jon Schneider
 * @author Nishant Raut
 * @author Brian Clozel
 * @since 2.0.0
 */
public final class RestTemplateExchangeTags {

	private static final Pattern STRIP_URI_PATTERN = Pattern.compile("^https?://[^/]+/");

	private RestTemplateExchangeTags() {
	}

	/**
	 * Creates a {@code method} {@code Tag} for the {@link HttpRequest#getMethod() method}
	 * of the given {@code request}.
	 * @param request the request
	 * @return the method tag
	 */
	public static Tag method(HttpRequest request) {
		return Tag.of("method", request.getMethod().name());
	}

	/**
	 * Creates a {@code uri} {@code Tag} for the URI of the given {@code request}.
	 * @param request the request
	 * @return the uri tag
	 */
	public static Tag uri(HttpRequest request) {
		return Tag.of("uri", ensureLeadingSlash(stripUri(request.getURI().toString())));
	}

	/**
	 * Creates a {@code uri} {@code Tag} from the given {@code uriTemplate}.
	 * @param uriTemplate the template
	 * @return the uri tag
	 */
	public static Tag uri(String uriTemplate) {
		String uri = (StringUtils.hasText(uriTemplate) ? uriTemplate : "none");
		return Tag.of("uri", ensureLeadingSlash(stripUri(uri)));
	}

	private static String stripUri(String uri) {
		return STRIP_URI_PATTERN.matcher(uri).replaceAll("");
	}

	private static String ensureLeadingSlash(String url) {
		return (url == null || url.startsWith("/")) ? url : "/" + url;
	}

	/**
	 * Creates a {@code status} {@code Tag} derived from the
	 * {@link ClientHttpResponse#getStatusCode() status} of the given {@code response}.
	 * @param response the response
	 * @return the status tag
	 */
	public static Tag status(ClientHttpResponse response) {
		return Tag.of("status", getStatusMessage(response));
	}

	private static String getStatusMessage(ClientHttpResponse response) {
		try {
			if (response == null) {
				return "CLIENT_ERROR";
			}
			return String.valueOf(response.getStatusCode().value());
		}
		catch (IOException ex) {
			return "IO_ERROR";
		}
	}

	/**
	 * Create a {@code client.name} {@code Tag} derived from the {@link URI#getHost host}
	 * of the {@link HttpRequest#getURI() URI} of the given {@code request}.
	 * @param request the request
	 * @return the client.name tag
	 */
	public static Tag clientName(HttpRequest request) {
		String host = request.getURI().getHost();
		if (host == null) {
			host = "none";
		}
		return Tag.of("client.name", host);
	}

	/**
	 * Creates an {@code outcome} {@code Tag} derived from the
	 * {@link ClientHttpResponse#getStatusCode() status} of the given {@code response}.
	 * @param response the response
	 * @return the outcome tag
	 * @since 2.2.0
	 */
	public static Tag outcome(ClientHttpResponse response) {
		try {
			if (response != null) {
				return Outcome.forStatus(response.getStatusCode().value()).asTag();
			}
		}
		catch (IOException ex) {
			// Continue
		}
		return Outcome.UNKNOWN.asTag();
	}

}
