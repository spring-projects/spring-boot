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

package org.springframework.bootstrap.actuate.endpoint.trace;

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
import org.springframework.bootstrap.actuate.trace.TraceRepository;
import org.springframework.core.Ordered;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * @author Dave Syer
 */
public class WebRequestLoggingFilter implements Filter, Ordered {

	final Log logger = LogFactory.getLog(WebRequestLoggingFilter.class);

	private boolean dumpRequests = false;

	private final TraceRepository traceRepository;

	private int order = Integer.MAX_VALUE;

	private ObjectMapper objectMapper = new ObjectMapper();

	/**
	 * @param traceRepository
	 */
	public WebRequestLoggingFilter(TraceRepository traceRepository) {
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

	public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
			throws IOException, ServletException {
		HttpServletRequest request = (HttpServletRequest) req;
		HttpServletResponse response = (HttpServletResponse) res;

		Map<String, Object> trace = getTrace(request);
		@SuppressWarnings("unchecked")
		Map<String, Object> headers = (Map<String, Object>) trace.get("headers");
		this.traceRepository.add(trace);
		if (this.logger.isTraceEnabled()) {
			this.logger.trace("Processing request " + request.getMethod() + " "
					+ request.getRequestURI());
			if (this.dumpRequests) {
				try {
					this.logger.trace("Headers: "
							+ this.objectMapper.writeValueAsString(headers));
				} catch (JsonProcessingException e) {
					throw new IllegalStateException("Cannot create JSON", e);
				}
			}
		}

		chain.doFilter(request, response);
	}

	protected Map<String, Object> getTrace(HttpServletRequest request) {

		Map<String, Object> map = new LinkedHashMap<String, Object>();
		Enumeration<String> names = request.getHeaderNames();

		while (names.hasMoreElements()) {
			String name = names.nextElement();
			List<String> values = Collections.list(request.getHeaders(name));
			Object value = values;
			if (values.size() == 1) {
				value = values.get(0);
			} else if (values.isEmpty()) {
				value = "";
			}
			map.put(name, value);

		}
		Map<String, Object> trace = new LinkedHashMap<String, Object>();
		trace.put("method", request.getMethod());
		trace.put("path", request.getRequestURI());
		trace.put("headers", map);
		return trace;
	}

	public void init(FilterConfig filterConfig) throws ServletException {
	}

	public void destroy() {
	}

}
