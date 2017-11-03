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
 * Intercepts incoming HTTP requests and records metrics about execution time and results.
 *
 * @author Jon Schneider
 * @since 2.0.0
 */
@Order(Ordered.HIGHEST_PRECEDENCE)
public class MetricsFilter extends OncePerRequestFilter {
	private final WebMvcMetrics webMvcMetrics;
	private final HandlerMappingIntrospector mappingIntrospector;
	private final Logger logger = LoggerFactory.getLogger(MetricsFilter.class);

	public MetricsFilter(WebMvcMetrics webMvcMetrics,
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
		HandlerExecutionChain handler;
		try {
			MatchableHandlerMapping matchableHandlerMapping = this.mappingIntrospector
					.getMatchableHandlerMapping(request);
			handler = matchableHandlerMapping.getHandler(request);
		}
		catch (Exception e) {
			this.logger.debug("Unable to time request", e);
			return;
		}

		if (handler != null) {
			Object handlerObject = handler.getHandler();
			this.webMvcMetrics.preHandle(request, handlerObject);
			try {
                filterChain.doFilter(request, response);

                // when an async operation is complete, the whole filter gets called
                // again with isAsyncStarted = false
                if (!request.isAsyncStarted()) {
                    this.webMvcMetrics.record(request, response, null);
                }
            }
            catch (NestedServletException e) {
                this.webMvcMetrics.record(request, response, e.getCause());
                throw e;
            }
		}
		else {
			filterChain.doFilter(request, response);
		}
	}
}
