/*
 * Copyright 2012-2013 the original author or authors.
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
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.boot.actuate.web.BasicErrorController;
import org.springframework.core.Ordered;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Servlet {@link Filter} that logs all requests to a {@link TraceRepository}.
 * 
 * @author Dave Syer
 */
public class WebRequestTraceFilter implements Filter, Ordered {

	private final Log logger = LogFactory.getLog(WebRequestTraceFilter.class);

	private boolean dumpRequests = false;

	private final TraceRepository traceRepository;

	private int order = Integer.MAX_VALUE;

	private final ObjectMapper objectMapper = new ObjectMapper();

	private BasicErrorController errorController;

	/**
	 * @param traceRepository
	 */
	public WebRequestTraceFilter(TraceRepository traceRepository) {
		this.traceRepository = traceRepository;
	}

	/**
	 * @param order the order to set
	 */
	public void setOrder(int order) {
		this.order = order;
	}

	@Override
	public int getOrder() {
		return this.order;
	}

	/**
	 * Debugging feature. If enabled, and trace logging is enabled then web request
	 * headers will be logged.
	 */
	public void setDumpRequests(boolean dumpRequests) {
		this.dumpRequests = dumpRequests;
	}

	@Override
	public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
			throws IOException, ServletException {
		HttpServletRequest request = (HttpServletRequest) req;
		HttpServletResponse response = (HttpServletResponse) res;

		Map<String, Object> trace = getTrace(request);
		if (this.logger.isTraceEnabled()) {
			this.logger.trace("Processing request " + request.getMethod() + " "
					+ request.getRequestURI());
			if (this.dumpRequests) {
				try {
					@SuppressWarnings("unchecked")
					Map<String, Object> headers = (Map<String, Object>) trace
							.get("headers");
					this.logger.trace("Headers: "
							+ this.objectMapper.writeValueAsString(headers));
				}
				catch (JsonProcessingException ex) {
					throw new IllegalStateException("Cannot create JSON", ex);
				}
			}
		}

		try {
			chain.doFilter(request, response);
		}
		finally {
			enhanceTrace(trace, response);
			this.traceRepository.add(trace);
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
		Throwable error = (Throwable) request
				.getAttribute("javax.servlet.error.exception");
		if (error != null) {
			if (this.errorController != null) {
				trace.put("error", this.errorController.extract(
						new ServletRequestAttributes(request), true, false));
			}
		}
		return trace;
	}

	@Override
	public void init(FilterConfig filterConfig) throws ServletException {
	}

	@Override
	public void destroy() {
	}

	public void setErrorController(BasicErrorController errorController) {
		this.errorController = errorController;
	}

}
