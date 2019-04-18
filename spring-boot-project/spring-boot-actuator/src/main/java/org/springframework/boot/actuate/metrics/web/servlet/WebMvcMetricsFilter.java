/*
 * Copyright 2012-2019 the original author or authors.
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
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import io.micrometer.core.annotation.Timed;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.Timer.Builder;
import io.micrometer.core.instrument.Timer.Sample;

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
 * @since 2.0.0
 */
public class WebMvcMetricsFilter extends OncePerRequestFilter {

	private final MeterRegistry registry;

	private final WebMvcTagsProvider tagsProvider;

	private final String metricName;

	private final boolean autoTimeRequests;

	private final double[] autoTimeRequestsPercentiles;

	private final boolean autoTimeRequestsHistogram;

	/**
	 * Create a new {@link WebMvcMetricsFilter} instance.
	 * @param registry the meter registry
	 * @param tagsProvider the tags provider
	 * @param metricName the metric name
	 * @param autoTimeRequests if requests should be automatically timed
	 * @since 2.0.7
	 * @deprecated since 2.1.4 in favor of
	 * {@link #WebMvcMetricsFilter(MeterRegistry, WebMvcTagsProvider, String, boolean, List, boolean)}
	 */
	@Deprecated
	public WebMvcMetricsFilter(MeterRegistry registry, WebMvcTagsProvider tagsProvider,
			String metricName, boolean autoTimeRequests) {
		this(registry, tagsProvider, metricName, autoTimeRequests, null, false);
	}

	/**
	 * Create a new {@link WebMvcMetricsFilter} instance.
	 * @param registry the meter registry
	 * @param tagsProvider the tags provider
	 * @param metricName the metric name
	 * @param autoTimeRequests if requests should be automatically timed
	 * @param autoTimeRequestsPercentiles default percentiles if requests are auto timed
	 * @param autoTimeRequestsHistogram default histogram flag if requests are auto timed
	 * @since 2.2.0
	 */
	public WebMvcMetricsFilter(MeterRegistry registry, WebMvcTagsProvider tagsProvider,
			String metricName, boolean autoTimeRequests,
			List<Double> autoTimeRequestsPercentiles, boolean autoTimeRequestsHistogram) {

		double[] percentiles = (autoTimeRequestsPercentiles != null)
				? autoTimeRequestsPercentiles.stream().mapToDouble(Double::doubleValue)
						.toArray()
				: null;

		this.registry = registry;
		this.tagsProvider = tagsProvider;
		this.metricName = metricName;
		this.autoTimeRequests = autoTimeRequests;
		this.autoTimeRequestsPercentiles = percentiles;
		this.autoTimeRequestsHistogram = autoTimeRequestsHistogram;
	}

	@Override
	protected boolean shouldNotFilterAsyncDispatch() {
		return false;
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request,
			HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {
		filterAndRecordMetrics(request, response, filterChain);
	}

	private void filterAndRecordMetrics(HttpServletRequest request,
			HttpServletResponse response, FilterChain filterChain)
			throws IOException, ServletException {
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
				Throwable exception = (Throwable) request
						.getAttribute(DispatcherServlet.EXCEPTION_ATTRIBUTE);
				record(timingContext, response, request, exception);
			}
		}
		catch (NestedServletException ex) {
			response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
			record(timingContext, response, request, ex.getCause());
			throw ex;
		}
		catch (ServletException | IOException | RuntimeException ex) {
			record(timingContext, response, request, ex);
			throw ex;
		}
	}

	private TimingContext startAndAttachTimingContext(HttpServletRequest request) {
		Timer.Sample timerSample = Timer.start(this.registry);
		TimingContext timingContext = new TimingContext(timerSample);
		timingContext.attachTo(request);
		return timingContext;
	}

	private Set<Timed> getTimedAnnotations(Object handler) {
		if (!(handler instanceof HandlerMethod)) {
			return Collections.emptySet();
		}
		return getTimedAnnotations((HandlerMethod) handler);
	}

	private Set<Timed> getTimedAnnotations(HandlerMethod handler) {
		Set<Timed> timed = findTimedAnnotations(handler.getMethod());
		if (timed.isEmpty()) {
			return findTimedAnnotations(handler.getBeanType());
		}
		return timed;
	}

	private Set<Timed> findTimedAnnotations(AnnotatedElement element) {
		return MergedAnnotations.from(element).stream(Timed.class)
				.collect(MergedAnnotationCollectors.toAnnotationSet());
	}

	private void record(TimingContext timingContext, HttpServletResponse response,
			HttpServletRequest request, Throwable exception) {
		Object handlerObject = request
				.getAttribute(HandlerMapping.BEST_MATCHING_HANDLER_ATTRIBUTE);
		Set<Timed> annotations = getTimedAnnotations(handlerObject);
		Timer.Sample timerSample = timingContext.getTimerSample();
		Supplier<Iterable<Tag>> tags = () -> this.tagsProvider.getTags(request, response,
				handlerObject, exception);
		if (annotations.isEmpty()) {
			if (this.autoTimeRequests) {
				stop(timerSample, tags, Timer.builder(this.metricName)
						.publishPercentiles(this.autoTimeRequestsPercentiles)
						.publishPercentileHistogram(this.autoTimeRequestsHistogram));
			}
		}
		else {
			for (Timed annotation : annotations) {
				stop(timerSample, tags, Timer.builder(annotation, this.metricName));
			}
		}
	}

	private void stop(Timer.Sample timerSample, Supplier<Iterable<Tag>> tags,
			Builder builder) {
		timerSample.stop(builder.tags(tags.get()).register(this.registry));
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

		public Timer.Sample getTimerSample() {
			return this.timerSample;
		}

		public void attachTo(HttpServletRequest request) {
			request.setAttribute(ATTRIBUTE, this);
		}

		public static TimingContext get(HttpServletRequest request) {
			return (TimingContext) request.getAttribute(ATTRIBUTE);
		}

	}

}
