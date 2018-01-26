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

import java.io.IOException;
import java.lang.reflect.AnnotatedElement;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import io.micrometer.core.annotation.Timed;
import io.micrometer.core.annotation.TimedSet;
import io.micrometer.core.instrument.LongTaskTimer;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.context.ApplicationContext;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.method.HandlerMethod;
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
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
public class MetricsFilter extends OncePerRequestFilter {

	private static final String EXCEPTION_ATTRIBUTE = "micrometer.requestException";

	private final MeterRegistry registry;

	private final ServletTagsProvider tagsProvider;

	private final String metricName;

	private final boolean autoTimeRequests;

	private final ApplicationContext context;

	private volatile HandlerMappingIntrospector mappingIntrospector;

	private final Logger logger = LoggerFactory.getLogger(MetricsFilter.class);

	/**
	 * Since the filter gets called twice for async requests, we need to hold the initial
	 * timing context until the second call.
	 */
	private final Map<HttpServletRequest, TimingSampleContext> asyncTimingContext = Collections
			.synchronizedMap(new IdentityHashMap<>());

	public MetricsFilter(MeterRegistry registry, ServletTagsProvider tagsProvider,
			String metricName, boolean autoTimeRequests, ApplicationContext context) {
		this.registry = registry;
		this.tagsProvider = tagsProvider;
		this.metricName = metricName;
		this.autoTimeRequests = autoTimeRequests;
		this.context = context;
	}

	public static void tagWithException(Throwable exception) {
		RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
		attributes.setAttribute(EXCEPTION_ATTRIBUTE, exception,
				RequestAttributes.SCOPE_REQUEST);
	}

	@Override
	protected boolean shouldNotFilterAsyncDispatch() {
		return false;
	}

	@SuppressWarnings("ConstantConditions")
	@Override
	protected void doFilterInternal(HttpServletRequest request,
			HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {
		HandlerExecutionChain handler = null;
		try {
			MatchableHandlerMapping matchableHandlerMapping = getMappingIntrospector()
					.getMatchableHandlerMapping(request);
			if (matchableHandlerMapping != null) {
				handler = matchableHandlerMapping.getHandler(request);
			}
		}
		catch (Exception e) {
			this.logger.debug("Unable to time request", e);
			filterChain.doFilter(request, response);
			return;
		}

		final Object handlerObject = handler == null ? null : handler.getHandler();

		// If this is the second invocation of the filter in an async request, we don't
		// want to start sampling again (effectively bumping the active count on any long
		// task timers).
		// Rather, we'll just use the sampling context we started on the first invocation.
		TimingSampleContext timingContext = this.asyncTimingContext.remove(request);
		if (timingContext == null) {
			timingContext = new TimingSampleContext(request, handlerObject);
		}

		try {
			filterChain.doFilter(request, response);

			if (request.isAsyncSupported()) {
				// this won't be "started" until after the first call to doFilter
				if (request.isAsyncStarted()) {
					this.asyncTimingContext.put(request, timingContext);
				}
			}

			if (!request.isAsyncStarted()) {
				record(timingContext, response, request, handlerObject,
						(Throwable) request.getAttribute(EXCEPTION_ATTRIBUTE));
			}
		}
		catch (NestedServletException e) {
			response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
			record(timingContext, response, request, handlerObject, e.getCause());
			throw e;
		}
	}

	private void record(TimingSampleContext timingContext, HttpServletResponse response,
			HttpServletRequest request, Object handlerObject, Throwable e) {
		for (Timed timedAnnotation : timingContext.timedAnnotations) {
			timingContext.timerSample.stop(Timer
					.builder(timedAnnotation, this.metricName).tags(this.tagsProvider
							.httpRequestTags(request, response, handlerObject, e))
					.register(this.registry));
		}

		if (timingContext.timedAnnotations.isEmpty() && autoTimeRequests) {
			timingContext.timerSample.stop(Timer
					.builder(this.metricName).tags(this.tagsProvider
							.httpRequestTags(request, response, handlerObject, e))
					.register(this.registry));
		}

		for (LongTaskTimer.Sample sample : timingContext.longTaskTimerSamples) {
			sample.stop();
		}
	}

	private HandlerMappingIntrospector getMappingIntrospector() {
		if (this.mappingIntrospector == null) {
			this.mappingIntrospector = this.context
					.getBean(HandlerMappingIntrospector.class);
		}
		return this.mappingIntrospector;
	}

	private class TimingSampleContext {
		private final Set<Timed> timedAnnotations;

		private final Timer.Sample timerSample;

		private final Collection<LongTaskTimer.Sample> longTaskTimerSamples;

		TimingSampleContext(HttpServletRequest request, Object handlerObject) {
			this.timedAnnotations = annotations(handlerObject);
			this.timerSample = Timer.start(MetricsFilter.this.registry);
			this.longTaskTimerSamples = this.timedAnnotations.stream()
					.filter(Timed::longTask)
					.map(t -> LongTaskTimer.builder(t)
							.tags(MetricsFilter.this.tagsProvider
									.httpLongRequestTags(request, handlerObject))
							.register(MetricsFilter.this.registry).start())
					.collect(Collectors.toList());
		}

		private Set<Timed> annotations(Object handler) {
			if (handler instanceof HandlerMethod) {
				HandlerMethod handlerMethod = (HandlerMethod) handler;
				Set<Timed> timed = findTimedAnnotations(handlerMethod.getMethod());
				if (timed.isEmpty()) {
					return findTimedAnnotations(handlerMethod.getBeanType());
				}
				return timed;
			}
			return Collections.emptySet();
		}

		private Set<Timed> findTimedAnnotations(AnnotatedElement element) {
			Timed t = AnnotationUtils.findAnnotation(element, Timed.class);
			// noinspection ConstantConditions
			if (t != null) {
				return Collections.singleton(t);
			}

			TimedSet ts = AnnotationUtils.findAnnotation(element, TimedSet.class);
			if (ts != null) {
				return Arrays.stream(ts.value()).collect(Collectors.toSet());
			}

			return Collections.emptySet();
		}
	}
}
