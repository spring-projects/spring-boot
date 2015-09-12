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

import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.boot.autoconfigure.web.ErrorAttributes;
import org.springframework.core.Ordered;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.filter.AbstractRequestLoggingFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

/**
 * Servlet {@link Filter} that logs all requests to a {@link TraceRepository}.
 *
 * @author Dave Syer
 * @author Wallace Wadge
 */
public class WebRequestTraceFilter extends AbstractRequestLoggingFilter implements Ordered {

	/** Key used in trace to store the request payload. */
	public static final String TRACE_RQ_KEY = "requestPayload";
	/** Key used in trace to store the response payload. */
	public static final String TRACE_RESP_KEY = "responsePayload";
	private static final String ONE_TIME_CHECK = "webRqFilter.beforeMessage.done";

	private final Log logger = LogFactory.getLog(WebRequestTraceFilter.class);

	private boolean dumpRequests = false;

	private boolean includePayloadResponse = false;

	// Not LOWEST_PRECEDENCE, but near the end, so it has a good chance of catching all
	// enriched headers, but users can add stuff after this if they want to
	private int order = Ordered.LOWEST_PRECEDENCE - 10;

	private final TraceRepository traceRepository;

	private ErrorAttributes errorAttributes;

	/**
	 * Create a new {@link WebRequestTraceFilter} instance.
	 * @param traceRepository the trace repository
	 */
	public WebRequestTraceFilter(TraceRepository traceRepository) {
		this.traceRepository = traceRepository;
	}

	/**
	 * Debugging feature. If enabled, and trace logging is enabled then web request
	 * headers will be logged.
	 * @param dumpRequests if requests should be logged
	 */
	public void setDumpRequests(boolean dumpRequests) {
		this.dumpRequests = dumpRequests;
	}

	/**
	 * If enabled, payload response is included as part of the trace (under responsePayload key).
	 * @param includePayloadResponse if response payload should be logged
	 */
	public void setIncludePayloadResponse(boolean includePayloadResponse) {
		this.includePayloadResponse = includePayloadResponse;
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

		HttpServletResponse responseToUse = response;

		if (!isAsyncDispatch(request) && !(response instanceof ContentCachingResponseWrapper)) {
			responseToUse = new ContentCachingResponseWrapper(response);
		}

		try {
			super.doFilterInternal(request, responseToUse, filterChain);
		}
		finally {
			// fetch the original trace again
			Map<String, Object> trace = (Map<String, Object>) request.getAttribute(TRACE_RQ_KEY);
			enhanceTrace(trace, responseToUse);

			if (this.includePayloadResponse) {
				trace.put(TRACE_RESP_KEY, createResponse(responseToUse));
			}

			this.traceRepository.add(trace);
		}
	}

	@Override
	protected String createMessage(HttpServletRequest request, String prefix, String suffix) {

		String result = "";
		if (request.getAttribute(ONE_TIME_CHECK) == null) {
			// a small check to prevent prematurely creating a request for nothing (before we're ready to consume)
			request.setAttribute(ONE_TIME_CHECK, true);
		}
		else {
			if (isIncludePayload() && request instanceof ContentCachingRequestWrapper) {
				ContentCachingRequestWrapper wrapper = (ContentCachingRequestWrapper) request;
				result = payloadBufferToString(wrapper.getCharacterEncoding(), wrapper.getContentAsByteArray());

				request.removeAttribute(ONE_TIME_CHECK);
			}
		}
		return result;
	}

	private String payloadBufferToString(String characterEncoding, byte[] buf1) {
		String payload = "";

		if (buf1.length > 0) {
			int length1 = Math.min(buf1.length, this.getMaxPayloadLength());

			try {
				payload = new String(buf1, 0, length1, characterEncoding);
			}
			catch (UnsupportedEncodingException uee) {
				payload = "[unknown]";
			}
		}

		return payload;
	}

	protected String createResponse(HttpServletResponse response) {
		String result = "";

		if (isIncludePayload() && response instanceof ContentCachingResponseWrapper) {
			ContentCachingResponseWrapper wrapper = (ContentCachingResponseWrapper) response;
			byte[] buf1 = wrapper.getContentAsByteArray();
			result = payloadBufferToString(wrapper.getCharacterEncoding(), buf1);
		}
		return result;
	}

	@Override
	protected void beforeRequest(HttpServletRequest request, String payload) {
		// content not read yet
	}

	@Override
	protected void afterRequest(HttpServletRequest request, String payload) {
		Map<String, Object> trace = getTrace(request, payload);

		if (this.logger.isTraceEnabled()) {
			this.logger.trace("Processing request " + request.getMethod() + " "
					+ request.getRequestURI());
			if (this.dumpRequests) {
				@SuppressWarnings("unchecked")
				Map<String, Object> headers = (Map<String, Object>) trace.get("headers");
				this.logger.trace("Headers: " + headers);
			}
		}

		request.setAttribute(TRACE_RQ_KEY, trace);
	}


	protected void enhanceTrace(Map<String, Object> trace,  HttpServletResponse response) {
		Map<String, String> headers = new LinkedHashMap<String, String>();
		for (String header : response.getHeaderNames()) {
			String value = response.getHeader(header);
			headers.put(header, value);
		}
		headers.put("status", "" + response.getStatus());
		@SuppressWarnings("unchecked")
		Map<String, Object> allHeaders = (Map<String, Object>) trace.get("headers");
		allHeaders.put("response", headers);
	}

	protected Map<String, Object> getTrace(HttpServletRequest request, String payload) {

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

		if (isIncludePayload()) {
			trace.put(TRACE_RQ_KEY, payload);
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
