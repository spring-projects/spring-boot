/*
 * Copyright 2012-2017 the original author or authors.
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

package org.springframework.boot.actuate.trace;

import java.io.IOException;
import java.security.Principal;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;
import javax.servlet.http.HttpSession;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.boot.web.servlet.error.ErrorAttributes;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Servlet {@link Filter} that logs all requests to a {@link TraceRepository}.
 *
 * @author Dave Syer
 * @author Wallace Wadge
 * @author Andy Wilkinson
 * @author Venil Noronha
 * @author Madhura Bhave
 */
public class WebRequestTraceFilter extends OncePerRequestFilter implements Ordered {

	private static final Log logger = LogFactory.getLog(WebRequestTraceFilter.class);

	private boolean dumpRequests = false;

	// Not LOWEST_PRECEDENCE, but near the end, so it has a good chance of catching all
	// enriched headers, but users can add stuff after this if they want to
	private int order = Ordered.LOWEST_PRECEDENCE - 10;

	private final TraceRepository repository;

	private ErrorAttributes errorAttributes;

	private final Set<Include> includes;

	/**
	 * Create a new {@link WebRequestTraceFilter} instance.
	 * @param repository the trace repository
	 * @param includes the {@link Include} to apply
	 */
	public WebRequestTraceFilter(TraceRepository repository, Set<Include> includes) {
		this.repository = repository;
		this.includes = includes;
	}

	/**
	 * Create a new {@link WebRequestTraceFilter} instance with the default
	 * {@link Include} to apply.
	 * @param repository the trace repository
	 * @see Include#defaultIncludes()
	 */
	public WebRequestTraceFilter(TraceRepository repository) {
		this(repository, Include.defaultIncludes());
	}

	/**
	 * Debugging feature. If enabled, and trace logging is enabled then web request
	 * headers will be logged.
	 * @param dumpRequests if requests should be logged
	 */
	public void setDumpRequests(boolean dumpRequests) {
		this.dumpRequests = dumpRequests;
	}

	@Override
	public int getOrder() {
		return this.order;
	}

	public void setOrder(int order) {
		this.order = order;
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request,
			HttpServletResponse response, FilterChain filterChain)
					throws ServletException, IOException {
		long startTime = System.nanoTime();
		Map<String, Object> trace = getTrace(request);
		logTrace(request, trace);
		int status = HttpStatus.INTERNAL_SERVER_ERROR.value();
		try {
			filterChain.doFilter(request, response);
			status = response.getStatus();
		}
		finally {
			addTimeTaken(trace, startTime);
			enhanceTrace(trace, status == response.getStatus() ? response
					: new CustomStatusResponseWrapper(response, status));
			this.repository.add(trace);
		}
	}

	protected Map<String, Object> getTrace(HttpServletRequest request) {
		HttpSession session = request.getSession(false);
		Throwable exception = (Throwable) request
				.getAttribute("javax.servlet.error.exception");
		Principal userPrincipal = request.getUserPrincipal();
		Map<String, Object> trace = new LinkedHashMap<>();
		Map<String, Object> headers = new LinkedHashMap<>();
		trace.put("method", request.getMethod());
		trace.put("path", request.getRequestURI());
		trace.put("headers", headers);
		if (isIncluded(Include.REQUEST_HEADERS)) {
			headers.put("request", getRequestHeaders(request));
		}
		add(trace, Include.PATH_INFO, "pathInfo", request.getPathInfo());
		add(trace, Include.PATH_TRANSLATED, "pathTranslated",
				request.getPathTranslated());
		add(trace, Include.CONTEXT_PATH, "contextPath", request.getContextPath());
		add(trace, Include.USER_PRINCIPAL, "userPrincipal",
				(userPrincipal == null ? null : userPrincipal.getName()));
		if (isIncluded(Include.PARAMETERS)) {
			trace.put("parameters", getParameterMapCopy(request));
		}
		add(trace, Include.QUERY_STRING, "query", request.getQueryString());
		add(trace, Include.AUTH_TYPE, "authType", request.getAuthType());
		add(trace, Include.REMOTE_ADDRESS, "remoteAddress", request.getRemoteAddr());
		add(trace, Include.SESSION_ID, "sessionId",
				(session == null ? null : session.getId()));
		add(trace, Include.REMOTE_USER, "remoteUser", request.getRemoteUser());
		if (isIncluded(Include.ERRORS) && exception != null
				&& this.errorAttributes != null) {
			trace.put("error", this.errorAttributes
					.getErrorAttributes(new ServletWebRequest(request), true));
		}
		return trace;
	}

	private Map<String, Object> getRequestHeaders(HttpServletRequest request) {
		Map<String, Object> headers = new LinkedHashMap<>();
		Set<String> excludedHeaders = getExcludeHeaders();
		Enumeration<String> names = request.getHeaderNames();
		while (names.hasMoreElements()) {
			String name = names.nextElement();
			if (!excludedHeaders.contains(name.toLowerCase())) {
				headers.put(name, getHeaderValue(request, name));
			}
		}
		postProcessRequestHeaders(headers);
		return headers;
	}

	private Set<String> getExcludeHeaders() {
		Set<String> excludedHeaders = new HashSet<>();
		if (!isIncluded(Include.COOKIES)) {
			excludedHeaders.add("cookie");
		}
		if (!isIncluded(Include.AUTHORIZATION_HEADER)) {
			excludedHeaders.add("authorization");
		}
		return excludedHeaders;
	}

	private Object getHeaderValue(HttpServletRequest request, String name) {
		List<String> value = Collections.list(request.getHeaders(name));
		if (value.size() == 1) {
			return value.get(0);
		}
		if (value.isEmpty()) {
			return "";
		}
		return value;
	}

	private Map<String, String[]> getParameterMapCopy(HttpServletRequest request) {
		return new LinkedHashMap<>(request.getParameterMap());
	}

	/**
	 * Post process request headers before they are added to the trace.
	 * @param headers a mutable map containing the request headers to trace
	 * @since 1.4.0
	 */
	protected void postProcessRequestHeaders(Map<String, Object> headers) {
	}

	private void addTimeTaken(Map<String, Object> trace, long startTime) {
		long timeTaken = System.nanoTime() - startTime;
		add(trace, Include.TIME_TAKEN, "timeTaken",
				"" + TimeUnit.NANOSECONDS.toMillis(timeTaken));
	}

	@SuppressWarnings("unchecked")
	protected void enhanceTrace(Map<String, Object> trace, HttpServletResponse response) {
		if (isIncluded(Include.RESPONSE_HEADERS)) {
			Map<String, Object> headers = (Map<String, Object>) trace.get("headers");
			headers.put("response", getResponseHeaders(response));
		}
	}

	private Map<String, String> getResponseHeaders(HttpServletResponse response) {
		Map<String, String> headers = new LinkedHashMap<>();
		for (String header : response.getHeaderNames()) {
			String value = response.getHeader(header);
			headers.put(header, value);
		}
		if (!isIncluded(Include.COOKIES)) {
			headers.remove("Set-Cookie");
		}
		headers.put("status", String.valueOf(response.getStatus()));
		return headers;
	}

	private void logTrace(HttpServletRequest request, Map<String, Object> trace) {
		if (logger.isTraceEnabled()) {
			logger.trace("Processing request " + request.getMethod() + " "
					+ request.getRequestURI());
			if (this.dumpRequests) {
				logger.trace("Headers: " + trace.get("headers"));
			}
		}
	}

	private void add(Map<String, Object> trace, Include include, String name,
			Object value) {
		if (isIncluded(include) && value != null) {
			trace.put(name, value);
		}
	}

	private boolean isIncluded(Include include) {
		return this.includes.contains(include);
	}

	public void setErrorAttributes(ErrorAttributes errorAttributes) {
		this.errorAttributes = errorAttributes;
	}

	private static final class CustomStatusResponseWrapper
			extends HttpServletResponseWrapper {

		private final int status;

		private CustomStatusResponseWrapper(HttpServletResponse response, int status) {
			super(response);
			this.status = status;
		}

		@Override
		public int getStatus() {
			return this.status;
		}

	}

}
