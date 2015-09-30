/*
 * Copyright 2012-2015 the original author or authors.
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
import java.io.UnsupportedEncodingException;
import java.security.Principal;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.boot.actuate.autoconfigure.TraceWebFilterProperties;
import org.springframework.boot.autoconfigure.web.ErrorAttributes;
import org.springframework.core.Ordered;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.context.request.async.WebAsyncManager;
import org.springframework.web.context.request.async.WebAsyncUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

/**
 * Servlet {@link Filter} that logs all requests to a {@link TraceRepository}.
 *
 * @author Dave Syer
 * @author Wallace Wadge
 */
public class WebRequestTraceFilter extends OncePerRequestFilter implements Ordered {
	/** Holds the request path info. */
	public static final String TRACE_RQ_PATH_INFO = "pathInfo";
	/** Holds the request path translated. */
	public static final String TRACE_RQ_PATH_TRANSLATED = "pathTranslated";
	/** Holds the request remote address. */
	public static final String TRACE_RQ_REMOTE_ADDR = "requestRemoteAddr";
	/** Holds the request context Path. */
	public static final String TRACE_RQ_CONTEXT_PATH = "contextPath";
	/** Holds the request user principal. */
	public static final String TRACE_RQ_USER_PRINCIPAL = "userPrincipal";
	/** Holds the request parameter map. */
	public static final String TRACE_RQ_PARAMS = "requestParams";
	/** Key used in trace to store the request auth type.  */
	public static final String TRACE_RQ_AUTH_TYPE = "requestAuthType";
	/** Key used in trace to store the request cookies.  */
	public static final String TRACE_RQ_COOKIES = "requestCookies";
	/** Key used in trace to store the request query string. */
	public static final String TRACE_RQ_QUERYSTRING = "requestQueryString";
	/** Key used in trace to store the request payload. */
	public static final String TRACE_RQ_PAYLOAD = "requestPayload";
	/** Key used in trace to store the response payload. */
	public static final String TRACE_RESP_PAYLOAD = "responsePayload";
	/** Holds session id. */
	public static final String TRACE_RQ_SESSION_ID = "requestSessionId";
	/** Holds request.getRemoteUser(). */
	public static final String TRACE_RQ_REMOTE_USER = "requestRemoteUser";

	private static final int DEFAULT_MAX_PAYLOAD_LENGTH = 50;


	private final Log logger = LogFactory.getLog(WebRequestTraceFilter.class);

	private boolean dumpRequests = false;

	private boolean includeAuthType = false;

	private boolean includeClientInfo = false;

	private boolean includeContextPath = false;

	private boolean includeCookies = false;

	private boolean includeParameters = false;

	private boolean includePathInfo = false;

	private boolean includePathTranslated = false;

	private boolean includePayload = false;

	private boolean includePayloadResponse = false;

	private boolean includeQueryString = false;

	private boolean includeUserPrincipal = false;

	private int maxPayloadLength = DEFAULT_MAX_PAYLOAD_LENGTH;

	private int maxPayloadResponseLength = DEFAULT_MAX_PAYLOAD_LENGTH;

	// Not LOWEST_PRECEDENCE, but near the end, so it has a good chance of catching all
	// enriched headers, but users can add stuff after this if they want to
	private int order = Ordered.LOWEST_PRECEDENCE - 10;

	private final TraceRepository traceRepository;

	private ErrorAttributes errorAttributes;

	/**
	 * Create a new {@link WebRequestTraceFilter} instance.
	 * @param traceRepository the trace repository.
	 */
	public WebRequestTraceFilter(TraceRepository traceRepository) {
		this.traceRepository = traceRepository;
	}

	public WebRequestTraceFilter(TraceRepository traceRepository, TraceWebFilterProperties traceWebFilterProperties) {
		this(traceRepository);
		this.includeAuthType = traceWebFilterProperties.isAuthType();
		this.includeClientInfo = traceWebFilterProperties.isClientInfo();
		this.includeContextPath = traceWebFilterProperties.isContextPath();
		this.includeCookies = traceWebFilterProperties.isCookies();
		this.includeParameters = traceWebFilterProperties.isParameters();
		this.includePathInfo = traceWebFilterProperties.isPathInfo();
		this.includePathTranslated = traceWebFilterProperties.isPathTranslated();
		this.includePayload = traceWebFilterProperties.isPayload();
		this.includePayloadResponse = traceWebFilterProperties.isPayloadResponse();
		this.includeQueryString = traceWebFilterProperties.isQueryString();
		this.includeUserPrincipal = traceWebFilterProperties.isUserPrincipal();
		this.includeUserPrincipal = traceWebFilterProperties.isUserPrincipal();
		this.maxPayloadLength = traceWebFilterProperties.getMaxPayloadLength();
		this.maxPayloadResponseLength = traceWebFilterProperties.getMaxPayloadResponseLength();
	}


	/**
	 * Set whether the query string should be included in the log message.
	 * <p>Should be configured using an {@code &lt;init-param&gt;} for parameter name
	 * includeQueryString" in the filter definition in {@code web.xml}.
	 * @param includeQueryString set to true to add to trace
	 */
	public void setIncludeQueryString(boolean includeQueryString) {
		this.includeQueryString = includeQueryString;
	}

	/**
	 * Return whether the query string should be included in the log message.
	 * @return true is query string is included
	 */
	protected boolean isIncludeQueryString() {
		return this.includeQueryString;
	}

	/**
	 * Set whether the client address and session id should be included in the
	 * log message.
	 * <p>Should be configured using an {@code &lt;init-param&gt;} for parameter name
	 * "includeClientInfo" in the filter definition in {@code web.xml}.
	 * @param includeClientInfo Set to true to include client/sessionId should be included
	 */
	public void setIncludeClientInfo(boolean includeClientInfo) {
		this.includeClientInfo = includeClientInfo;
	}

	/**
	 * Return whether the client address and session id should be included in the
	 * log message.
	 * @return true if client address/session id are included
	 */
	protected boolean isIncludeClientInfo() {
		return this.includeClientInfo;
	}

	/**
	 * Set whether the request payload (body) should be included in the log message.
	 * <p>Should be configured using an {@code &lt;init-param&gt;} for parameter name
	 * "includePayload" in the filter definition in {@code web.xml}.
	 * @param includePayload set to true to include the request payload.
	 */
	public void setIncludePayload(boolean includePayload) {
		this.includePayload = includePayload;
	}

	/**
	 * Return whether the request payload (body) should be included in the log message.
	 * @return true if request payload is included
	 */
	protected boolean isIncludePayload() {
		return this.includePayload;
	}

	/**
	 * Sets the maximum length of the payload body to be included in the log message.
	 * Default is 50 characters.
	 * @param maxPayloadLength Limit the number of bytes to capture for request/response
	 */
	public void setMaxPayloadLength(int maxPayloadLength) {
		Assert.isTrue(maxPayloadLength >= 0, "'maxPayloadLength' should be larger than or equal to 0");
		this.maxPayloadLength = maxPayloadLength;
	}

	/**
	 * Return the maximum length of the payload body to be included in the log message.
	 * @return payload length that will be captured
	 */
	protected int getMaxPayloadLength() {
		return this.maxPayloadLength;
	}


	/**
	 * Sets the maximum length of the payload response body to be included in the log message.
	 * Default is 50 characters.
	 * @param maxPayloadResponseLength Limit the number of bytes to capture for request/response
	 */
	public void setMaxPayloadResponseLength(int maxPayloadResponseLength) {
		Assert.isTrue(maxPayloadResponseLength >= 0, "'maxPayloadResponseLength' should be larger than or equal to 0");
		this.maxPayloadResponseLength = maxPayloadResponseLength;
	}

	/**
	 * Return the maximum length of the payload response body to be included in the log message.
	 * @return payload length that will be captured
	 */
	protected int getMaxPayloadResponseLength() {
		return this.maxPayloadResponseLength;
	}

	/**
	 * Debugging feature. If enabled, and trace logging is enabled then web request
	 * headers will be logged.
	 * @param dumpRequests if requests should be logged.
	 */
	public void setDumpRequests(boolean dumpRequests) {
		this.dumpRequests = dumpRequests;
	}

	/**
	 * If enabled, includePathInfo is included as part of the trace.
	 * @param includePathInfo if pathInfo should be set in the trace.
	 */
	public void setIncludePathInfo(boolean includePathInfo) {
		this.includePathInfo = includePathInfo;
	}

	/**
	 * If enabled, userPrinciple is included as part of the trace.
	 * @param includeUserPrincipal if pathInfo should be set in the trace.
	 */
	public void setIncludeUserPrincipal(boolean includeUserPrincipal) {
		this.includeUserPrincipal = includeUserPrincipal;
	}


	/**
	 * If enabled, contextPath is included as part of the trace.
	 * @param includeContextPath if pathInfo should be set in the trace.
	 */
	public void setIncludeContextPath(boolean includeContextPath) {
		this.includeContextPath = includeContextPath;
	}


	/**
	 * If enabled, pathTranslated is included as part of the trace.
	 * @param includePathTranslated if pathInfo should be set in the trace.
	 */
	public void setIncludePathTranslated(boolean includePathTranslated) {
		this.includePathTranslated = includePathTranslated;
	}


	/**
	 * If enabled, payload response is included as part of the trace (under responsePayload key).
	 * @param includePayloadResponse if response payload should be logged.
	 */
	public void setIncludePayloadResponse(boolean includePayloadResponse) {
		this.includePayloadResponse = includePayloadResponse;
	}

	/**
	 * If enabled, auth type is included as part of the trace.
	 * @param includeAuthType if auth type should be set in the trace.
	 */
	public void setIncludeAuthType(boolean includeAuthType) {
		this.includeAuthType = includeAuthType;
	}

	/**
	 * If enabled, cookies are included as part of the trace.
	 * @param includeCookies if cookies should be set in the trace.
	 */
	public void setIncludeCookies(boolean includeCookies) {
		this.includeCookies = includeCookies;
	}

	/**
	 * If enabled, request parameters are included as part of the trace.
	 * @param includeParameters if response payload should be logged.
	 */
	public void setIncludeParameters(boolean includeParameters) {
		this.includeParameters = includeParameters;
	}

	/** Returns if web request headers will be logged.
	 * @return true if request headers will be logged when tracing is enabled. */
	protected boolean isDumpRequests() {
		return this.dumpRequests;
	}


	/**
	 * Returns whether auth type is added to the trace.
	 * @return true if auth type is added.
	 */
	protected boolean isIncludeAuthType() {
		return this.includeAuthType;
	}

	/**
	 * Returns whether payload response is added to the trace.
	 * @return true if payload is added.
	 */
	protected boolean isIncludePayloadResponse() {
		return this.includePayloadResponse;
	}

	/** Returns whether query parameters are included in the trace.
	 * @return true if they are included.
	 */
	protected boolean isIncludeParameters() {
		return this.includeParameters;
	}

	/** Returns whether cookies are included in the trace.
	 * @return true if they are included.
	 */
	protected boolean isIncludeCookies() {
		return this.includeCookies;
	}

	/** Returns whether pathInfo is included in the trace.
	 * @return true if included.
	 */
	protected boolean isIncludePathInfo() {
		return this.includePathInfo;
	}

	/** Returns whether userPrincipal is included in the trace.
	 * @return true if included.
	 */
	protected boolean isIncludeUserPrincipal() {
		return this.includeUserPrincipal;
	}

	/** Returns whether pathTranslated is included in the trace.
	 * @return true if included.
	 */
	protected boolean isIncludePathTranslated() {
		return this.includePathTranslated;
	}

	/** Returns whether contextPath is included in the trace.
	 * @return true if included.
	 */
	protected boolean isIncludeContextPath() {
		return this.includeContextPath;
	}



	@Override
	public int getOrder() {
		return this.order;
	}

	public void setOrder(int order) {
		this.order = order;
	}

	/**
	 * The dispatcher type {@code javax.servlet.DispatcherType.ASYNC} introduced
	 * in Servlet 3.0 means a filter can be invoked in more than one thread over
	 * the course of a single request. This method returns {@code true} if the
	 * filter is currently executing within an asynchronous dispatch.
	 * @param request the current request
	 * @return true if we are in an async dispatch
	 * @since 3.2
	 * @see WebAsyncManager#hasConcurrentResult()
	 */
	protected boolean isAsyncDispatch(HttpServletRequest request) {
		return WebAsyncUtils.getAsyncManager(request).hasConcurrentResult();
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request,
									HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {

		HttpServletRequest requestToUse = request;
		HttpServletResponse responseToUse = response;

		boolean isFirstRequest = !isAsyncDispatch(request);

		if (isIncludePayload() && isFirstRequest && !(request instanceof ContentCachingRequestWrapper)) {
			requestToUse = new ContentCachingRequestWrapper(request);
		}

		if (isIncludePayloadResponse() && !isAsyncDispatch(request) && !(response instanceof ContentCachingResponseWrapper)) {
			responseToUse = new ContentCachingResponseWrapper(response);
		}

		Map<String, Object> trace = getTrace(requestToUse);
		if (this.logger.isTraceEnabled()) {
			this.logger.trace("Processing request " + requestToUse.getMethod() + " "
					+ requestToUse.getRequestURI());
			if (this.dumpRequests) {
				@SuppressWarnings("unchecked")
				Map<String, Object> headers = (Map<String, Object>) trace.get("headers");
				this.logger.trace("Headers: " + headers);
			}
		}

		try {
			filterChain.doFilter(requestToUse, responseToUse);
		}
		finally {
			enhanceTraceRequest(trace, requestToUse);
			enhanceTrace(trace, responseToUse);
			this.traceRepository.add(trace);
		}
	}

	protected void enhanceTraceRequest(Map<String, Object> trace, HttpServletRequest request) {
		if (isIncludePayload() && request instanceof ContentCachingRequestWrapper) {
			ContentCachingRequestWrapper wrapper = (ContentCachingRequestWrapper) request;
			trace.put(TRACE_RQ_PAYLOAD, payloadBufferToString(wrapper.getCharacterEncoding(), wrapper.getContentAsByteArray(), this.getMaxPayloadLength()));
		}
	}

	protected void enhanceTrace(Map<String, Object> trace, HttpServletResponse response) {
		Map<String, String> headers = new LinkedHashMap<String, String>();
		for (String header : response.getHeaderNames()) {
			String value = response.getHeader(header);
			headers.put(header, value);
		}
		headers.put("status", "" + response.getStatus());
		@SuppressWarnings("unchecked")
		Map<String, Object> allHeaders = (Map<String, Object>) trace.get("headers");
		allHeaders.put("response", headers);


		if (isIncludePayload() && response instanceof ContentCachingResponseWrapper) {
			ContentCachingResponseWrapper wrapper = (ContentCachingResponseWrapper) response;
			byte[] buf1 = wrapper.getContentAsByteArray();
			trace.put(TRACE_RESP_PAYLOAD, payloadBufferToString(wrapper.getCharacterEncoding(), buf1, this.getMaxPayloadResponseLength()));
		}
	}

	private String payloadBufferToString(String characterEncoding, byte[] buf1, int length) {
		String payload = "";

		if (buf1.length > 0) {
			int length1 = Math.min(buf1.length, length);

			try {
				payload = new String(buf1, 0, length1, characterEncoding);
			}
			catch (UnsupportedEncodingException uee) {
				payload = "[unknown]";
			}
		}

		return payload;
	}



	protected Map<String, Object> getTrace(HttpServletRequest request) {

		Map<String, Object> headers = new LinkedHashMap<String, Object>();
		Enumeration<String> names = request.getHeaderNames();

		while (names.hasMoreElements()) {
			String name = names.nextElement();
			List<String> values = Collections.list(request.getHeaders(name));
			Object value = values;
			if (values.size() == 1) {
				value = values.get(0);
			}
			else if (values.isEmpty()) {
				value = "";
			}
			headers.put(name, value);

		}
		Map<String, Object> trace = new LinkedHashMap<String, Object>();
		Map<String, Object> allHeaders = new LinkedHashMap<String, Object>();
		allHeaders.put("request", headers);
		trace.put("method", request.getMethod());
		trace.put("path", request.getRequestURI());
		trace.put("headers", allHeaders);

		if (isIncludePathInfo()) {
			String pathInfo = request.getPathInfo();
			if (pathInfo != null) {
				trace.put(TRACE_RQ_PATH_INFO, pathInfo);
			}
		}

		if (isIncludePathTranslated()) {
			String pathTranslated = request.getPathTranslated();
			if (pathTranslated != null) {
				trace.put(TRACE_RQ_PATH_TRANSLATED, pathTranslated);
			}
		}
		if (isIncludeContextPath()) {
			String contextPath = request.getContextPath();
			if (contextPath != null) {
				trace.put(TRACE_RQ_CONTEXT_PATH, contextPath);
			}
		}

		if (isIncludeUserPrincipal()) {
			Principal principal = request.getUserPrincipal();
			if (principal != null) {
				trace.put(TRACE_RQ_USER_PRINCIPAL, principal);
			}
		}


		if (isIncludeParameters()) {
			trace.put(TRACE_RQ_PARAMS, request.getParameterMap());
		}

		if (isIncludeQueryString()) {
			String queryString = request.getQueryString();
			if (queryString != null) {
				trace.put(TRACE_RQ_QUERYSTRING, queryString);
			}
		}

		if (isIncludeCookies()) {
			Cookie[] cookies = request.getCookies();
			if (cookies != null) {
				trace.put(TRACE_RQ_COOKIES, cookies);
			}
		}

		if (isIncludeAuthType()) {
			String authType = request.getAuthType();
			if (authType != null) {
				trace.put(TRACE_RQ_AUTH_TYPE, authType);
			}
		}


		if (isIncludeQueryString()) {
			trace.put(TRACE_RQ_QUERYSTRING, request.getQueryString());
		}



		if (isIncludeClientInfo()) {
			String client = request.getRemoteAddr();
			if (StringUtils.hasLength(client)) {
				trace.put(TRACE_RQ_REMOTE_ADDR, client);
			}
			HttpSession session = request.getSession(false);
			if (session != null) {
				trace.put(TRACE_RQ_SESSION_ID, session.getId());
			}
			String user = request.getRemoteUser();
			if (user != null) {
				trace.put(TRACE_RQ_REMOTE_USER, user);
			}


		}

		Throwable exception = (Throwable) request
				.getAttribute("javax.servlet.error.exception");
		if (exception != null && this.errorAttributes != null) {
			RequestAttributes requestAttributes = new ServletRequestAttributes(request);
			Map<String, Object> error = this.errorAttributes.getErrorAttributes(
					requestAttributes, true);
			trace.put("error", error);
		}
		return trace;
	}

	public void setErrorAttributes(ErrorAttributes errorAttributes) {
		this.errorAttributes = errorAttributes;
	}

}
