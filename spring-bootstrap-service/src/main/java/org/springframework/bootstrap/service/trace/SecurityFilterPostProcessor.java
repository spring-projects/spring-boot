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
package org.springframework.bootstrap.service.trace;

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
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.util.Assert;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Bean post processor that adds a filter to Spring Security. The filter (optionally) logs
 * request headers at trace level and also sends the headers to a {@link TraceRepository}
 * for later analysis.
 * 
 * @author Luke Taylor
 * @author Dave Syer
 * 
 */
public class SecurityFilterPostProcessor implements BeanPostProcessor {

	private final static Log logger = LogFactory
			.getLog(SecurityFilterPostProcessor.class);
	private boolean dumpRequests = false;
	private List<String> ignore = Collections.emptyList();

	private TraceRepository traceRepository = new InMemoryTraceRepository();

	/**
	 * @param traceRepository
	 */
	public SecurityFilterPostProcessor(TraceRepository traceRepository) {
		super();
		this.traceRepository = traceRepository;
	}

	/**
	 * List of filter chains which should be ignored completely.
	 */
	public void setIgnore(List<String> ignore) {
		Assert.notNull(ignore);
		this.ignore = ignore;
	}

	/**
	 * Debugging feature. If enabled, and trace logging is enabled
	 */
	public void setDumpRequests(boolean dumpRequests) {
		this.dumpRequests = dumpRequests;
	}

	@Override
	public Object postProcessAfterInitialization(Object bean, String beanName)
			throws BeansException {

		if (!this.ignore.contains(beanName)) {
			if (bean instanceof FilterChainProxy) {
				FilterChainProxy proxy = (FilterChainProxy) bean;
				for (SecurityFilterChain filterChain : proxy.getFilterChains()) {
					processFilterChain(filterChain, beanName);
				}
			}
			if (bean instanceof SecurityFilterChain) {
				processFilterChain((SecurityFilterChain) bean, beanName);
			}
		}

		return bean;

	}

	private void processFilterChain(SecurityFilterChain filterChain, String beanName) {
		logger.info("Processing security filter chain " + beanName);
		Filter loggingFilter = new WebRequestLoggingFilter(beanName);
		filterChain.getFilters().add(0, loggingFilter);
	}

	@Override
	public Object postProcessBeforeInitialization(Object bean, String beanName)
			throws BeansException {
		return bean;
	}

	class WebRequestLoggingFilter implements Filter {

		final Log logger = LogFactory.getLog(WebRequestLoggingFilter.class);
		private final String name;
		private ObjectMapper objectMapper = new ObjectMapper();

		WebRequestLoggingFilter(String name) {
			this.name = name;
		}

		public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
				throws IOException, ServletException {
			HttpServletRequest request = (HttpServletRequest) req;
			HttpServletResponse response = (HttpServletResponse) res;

			Map<String, Object> trace = getTrace(request);
			@SuppressWarnings("unchecked")
			Map<String, Object> headers = (Map<String, Object>) trace.get("headers");
			SecurityFilterPostProcessor.this.traceRepository.add(trace);
			if (this.logger.isTraceEnabled()) {
				this.logger.trace("Filter chain '" + this.name + "' processing request "
						+ request.getMethod() + " " + request.getRequestURI());
				if (SecurityFilterPostProcessor.this.dumpRequests) {
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
			trace.put("chain", this.name);
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

}
