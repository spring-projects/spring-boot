/*
 * Copyright 2012-2021 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
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
import java.util.Collections;
import java.util.Set;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import io.micrometer.core.annotation.Timed;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.Timer.Builder;
import io.micrometer.core.instrument.Timer.Sample;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.boot.actuate.metrics.AutoTimer;
import org.springframework.core.annotation.MergedAnnotationCollectors;
import org.springframework.core.annotation.MergedAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.DispatcherServlet;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.util.NestedServletException;

/**
 * Intercepts incoming HTTP requests and records metrics about Spring MVC execution time
 * and results.
 *
 * @author Jon Schneider
 * @author Phillip Webb
 * @author Chanhyeong LEE
 * @since 2.0.0
 */
public class WebMvcMetricsFilter extends OncePerRequestFilter {

	private static final Log logger = LogFactory.getLog(WebMvcMetricsFilter.class);

	private final MeterRegistry registry;

	private final WebMvcTagsProvider tagsProvider;

	private final String metricName;

	private final AutoTimer autoTimer;

	/**
	 * Create a new {@link WebMvcMetricsFilter} instance.
	 * @param registry the meter registry
	 * @param tagsProvider the tags provider
	 * @param metricName the metric name
	 * @param autoTimer the auto-timers to apply or {@code null} to disable auto-timing
	 * @since 2.2.0
	 */
	public WebMvcMetricsFilter(MeterRegistry registry, WebMvcTagsProvider tagsProvider, String metricName,
			AutoTimer autoTimer) {
		this.registry = registry;
		this.tagsProvider = tagsProvider;
		this.metricName = metricName;
		this.autoTimer = autoTimer;
	}

	@Override
	protected boolean shouldNotFilterAsyncDispatch() {
		return false;
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {
		TimingContext timingContext = TimingContext.get(request);
		if (timingContext == null) {
			timingContext = startAndAttachTimingContext(request);
		}
		try {
			filterChain.doFilter(request, response);
			if (!request.isAsyncStarted()) {
				// Only record when async processing has finished or never been started.
				// If async was started by something further down the chain we wait
				// until the second filter invocation (but we'll be using the
				// TimingContext that was attached to the first)
				Throwable exception = (Throwable) request.getAttribute(DispatcherServlet.EXCEPTION_ATTRIBUTE);
				record(timingContext, request, response, exception);
			}
		}
		catch (NestedServletException ex) {
			response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
			record(timingContext, request, response, ex.getCause());
			throw ex;
		}
		catch (ServletException | IOException | RuntimeException ex) {
			record(timingContext, request, response, ex);
			throw ex;
		}
	}

	private TimingContext startAndAttachTimingContext(HttpServletRequest request) {
		Timer.Sample timerSample = Timer.start(this.registry);
		TimingContext timingContext = new TimingContext(timerSample);
		timingContext.attachTo(request);
		return timingContext;
	}

	private void record(TimingContext timingContext, HttpServletRequest request, HttpServletResponse response,
			Throwable exception) {
		try {
			Object handler = getHandler(request);
			Set<Timed> annotations = getTimedAnnotations(handler);
			Timer.Sample timerSample = timingContext.getTimerSample();
			if (annotations.isEmpty()) {
				if (this.autoTimer.isEnabled()) {
					Builder builder = this.autoTimer.builder(this.metricName);
					timerSample.stop(getTimer(builder, handler, request, response, exception));
				}
			}
			else {
				for (Timed annotation : annotations) {
					Builder builder = Timer.builder(annotation, this.metricName);
					timerSample.stop(getTimer(builder, handler, request, response, exception));
				}
			}
		}
		catch (Exception ex) {
			logger.warn("Failed to record timer metrics", ex);
			// Allow request-response exchange to continue, unaffected by metrics problem
		}
	}

	private Object getHandler(HttpServletRequest request) {
		return request.getAttribute(HandlerMapping.BEST_MATCHING_HANDLER_ATTRIBUTE);
	}

	private Set<Timed> getTimedAnnotations(Object handler) {
		if (!(handler instanceof HandlerMethod)) {
			return Collections.emptySet();
		}
		return getTimedAnnotations((HandlerMethod) handler);
	}

	private Set<Timed> getTimedAnnotations(HandlerMethod handler) {
		Set<Timed> methodAnnotations = findTimedAnnotations(handler.getMethod());
		if (!methodAnnotations.isEmpty()) {
			return methodAnnotations;
		}
		return findTimedAnnotations(handler.getBeanType());
	}

	private Set<Timed> findTimedAnnotations(AnnotatedElement element) {
		MergedAnnotations annotations = MergedAnnotations.from(element);
		if (!annotations.isPresent(Timed.class)) {
			return Collections.emptySet();
		}
		return annotations.stream(Timed.class).collect(MergedAnnotationCollectors.toAnnotationSet());
	}

	private Timer getTimer(Builder builder, Object handler, HttpServletRequest request, HttpServletResponse response,
			Throwable exception) {
		return builder.tags(this.tagsProvider.getTags(request, response, handler, exception)).register(this.registry);
	}

	/**
	 * Context object attached to a request to retain information across the multiple
	 * filter calls that happen with async requests.
	 */
	private static class TimingContext {

		private static final String ATTRIBUTE = TimingContext.class.getName();

		private final Timer.Sample timerSample;

		TimingContext(Sample timerSample) {
			this.timerSample = timerSample;
		}

		Timer.Sample getTimerSample() {
			return this.timerSample;
		}

		void attachTo(HttpServletRequest request) {
			request.setAttribute(ATTRIBUTE, this);
		}

		static TimingContext get(HttpServletRequest request) {
			return (TimingContext) request.getAttribute(ATTRIBUTE);
		}

	}

}
