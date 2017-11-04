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

package org.springframework.boot.actuate.metrics.web.servlet;

import java.io.IOException;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerExecutionChain;
import org.springframework.web.servlet.handler.HandlerMappingIntrospector;
import org.springframework.web.servlet.handler.MatchableHandlerMapping;
import org.springframework.web.util.NestedServletException;

/**
 * Intercepts incoming HTTP requests and records metrics about Spring MVC execution time
 * and results.
 *
 * @author Jon Schneider
 * @since 2.0.0
 */
@Order(Ordered.HIGHEST_PRECEDENCE)
public class WebMvcMetricsFilter extends OncePerRequestFilter {

	private static final Logger logger = LoggerFactory
			.getLogger(WebMvcMetricsFilter.class);

	private final WebMvcMetrics webMvcMetrics;

	private final HandlerMappingIntrospector mappingIntrospector;

	public WebMvcMetricsFilter(WebMvcMetrics webMvcMetrics,
			HandlerMappingIntrospector mappingIntrospector) {
		this.webMvcMetrics = webMvcMetrics;
		this.mappingIntrospector = mappingIntrospector;
	}

	@Override
	protected boolean shouldNotFilterAsyncDispatch() {
		return false;
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request,
			HttpServletResponse response, FilterChain filterChain)
					throws ServletException, IOException {
		HandlerExecutionChain handlerExecutionChain = getHandlerExecutionChain(request);
		Object handler = (handlerExecutionChain == null ? null
				: handlerExecutionChain.getHandler());
		filterWithMetrics(request, response, filterChain, handler);
	}

	private HandlerExecutionChain getHandlerExecutionChain(HttpServletRequest request) {
		try {
			MatchableHandlerMapping matchableHandlerMapping = this.mappingIntrospector
					.getMatchableHandlerMapping(request);
			return (matchableHandlerMapping == null ? null
					: matchableHandlerMapping.getHandler(request));
		}
		catch (Exception ex) {
			if (logger.isDebugEnabled()) {
				logger.debug("Unable to time request", ex);
			}
			return null;
		}
	}

	private void filterWithMetrics(HttpServletRequest request,
			HttpServletResponse response, FilterChain filterChain, Object handler)
					throws IOException, ServletException, NestedServletException {
		this.webMvcMetrics.preHandle(request, handler);
		try {
			filterChain.doFilter(request, response);
			// When an async operation is complete, the whole filter gets called again
			// with isAsyncStarted = false
			if (!request.isAsyncStarted()) {
				this.webMvcMetrics.record(request, response, null);
			}
		}
		catch (NestedServletException ex) {
			this.webMvcMetrics.record(request, response, ex.getCause());
			throw ex;
		}
	}

}
