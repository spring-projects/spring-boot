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

package org.springframework.boot.actuate.metrics.web.servlet;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import io.micrometer.core.instrument.Tag;

import org.springframework.http.HttpStatus;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerMapping;

/**
 * Factory methods for {@link Tag Tags} associated with a request-response exchange that
 * is handled by Spring MVC.
 *
 * @author Jon Schneider
 * @author Andy Wilkinson
 * @author Brian Clozel
 * @since 2.0.0
 */
public final class WebMvcTags {

	private static final Tag URI_NOT_FOUND = Tag.of("uri", "NOT_FOUND");

	private static final Tag URI_REDIRECTION = Tag.of("uri", "REDIRECTION");

	private static final Tag URI_UNKNOWN = Tag.of("uri", "UNKNOWN");

	private static final Tag EXCEPTION_NONE = Tag.of("exception", "None");

	private static final Tag STATUS_UNKNOWN = Tag.of("status", "UNKNOWN");

	private static final Tag METHOD_UNKNOWN = Tag.of("method", "UNKNOWN");

	private WebMvcTags() {
	}

	/**
	 * Creates a {@code method} tag based on the {@link HttpServletRequest#getMethod()
	 * method} of the given {@code request}.
	 * @param request the request
	 * @return the method tag whose value is a capitalized method (e.g. GET).
	 */
	public static Tag method(HttpServletRequest request) {
		return (request == null ? METHOD_UNKNOWN : Tag.of("method", request.getMethod()));
	}

	/**
	 * Creates a {@code method} tag based on the status of the given {@code response}.
	 * @param response the HTTP response
	 * @return the status tag derived from the status of the response
	 */
	public static Tag status(HttpServletResponse response) {
		return (response == null ? STATUS_UNKNOWN :
				Tag.of("status", Integer.toString(response.getStatus())));
	}

	/**
	 * Creates a {@code uri} tag based on the URI of the given {@code request}. Uses the
	 * {@link HandlerMapping#BEST_MATCHING_PATTERN_ATTRIBUTE} best matching pattern if
	 * available, falling back to the request's {@link HttpServletRequest#getPathInfo()
	 * path info} if necessary.
	 * @param request the request
	 * @param response the response
	 * @return the uri tag derived from the request
	 */
	public static Tag uri(HttpServletRequest request, HttpServletResponse response) {
		if (request != null) {
			String pattern = getMatchingPattern(request);
			if (pattern != null) {
				return Tag.of("uri", pattern);
			}
			else if (response != null) {
				HttpStatus status = extractStatus(response);
				if (status != null && status.is3xxRedirection()) {
					return URI_REDIRECTION;
				}
				if (status != null && status.equals(HttpStatus.NOT_FOUND)) {
					return URI_NOT_FOUND;
				}
			}
			String pathInfo = getPathInfo(request);
			return Tag.of("uri", pathInfo.isEmpty() ? "root" : pathInfo);
		}
		return URI_UNKNOWN;
	}

	private static HttpStatus extractStatus(HttpServletResponse response) {
		try {
			return HttpStatus.valueOf(response.getStatus());
		}
		catch (IllegalArgumentException ex) {
			return null;
		}
	}

	private static String getMatchingPattern(HttpServletRequest request) {
		return (String) request
				.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
	}

	private static String getPathInfo(HttpServletRequest request) {
		String pathInfo = request.getPathInfo();
		String uri = (StringUtils.hasText(pathInfo) ? pathInfo : "/");
		return uri.replaceAll("//+", "/").replaceAll("/$", "");
	}

	/**
	 * Creates a {@code exception} tag based on the {@link Class#getSimpleName() simple
	 * name} of the class of the given {@code exception}.
	 * @param exception the exception, may be {@code null}
	 * @return the exception tag derived from the exception
	 */
	public static Tag exception(Throwable exception) {
		return (exception != null
				? Tag.of("exception", exception.getClass().getSimpleName())
				: EXCEPTION_NONE);
	}

}
