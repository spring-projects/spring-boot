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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;

import io.micrometer.core.annotation.Timed;
import io.micrometer.core.instrument.LongTaskTimer;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.Timer.Builder;
import io.micrometer.core.instrument.Timer.Sample;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.context.ApplicationContext;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.DispatcherServlet;
import org.springframework.web.servlet.HandlerExecutionChain;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.servlet.handler.HandlerMappingIntrospector;
import org.springframework.web.servlet.handler.MatchableHandlerMapping;
import org.springframework.web.util.NestedServletException;

/**
 * Intercepts incoming HTTP requests and records metrics about Spring MVC execution time
 * and results.
 *
 * @author Jon Schneider
 * @author Phillip Webb
 * @since 2.0.0
 */
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
public class WebMvcMetricsFilter extends OncePerRequestFilter {

	private static final Logger logger = LoggerFactory
			.getLogger(WebMvcMetricsFilter.class);

	private final ApplicationContext context;

	private final MeterRegistry registry;

	private final WebMvcTagsProvider tagsProvider;

	private final String metricName;

	private final boolean autoTimeRequests;

	private volatile HandlerMappingIntrospector introspector;

	/**
	 * Create a new {@link WebMvcMetricsFilter} instance.
	 * @param context the source application context
	 * @param registry the meter registry
	 * @param tagsProvider the tags provider
	 * @param metricName the metric name
	 * @param autoTimeRequests if requests should be automatically timed
	 */
	public WebMvcMetricsFilter(ApplicationContext context, MeterRegistry registry,
			WebMvcTagsProvider tagsProvider, String metricName,
			boolean autoTimeRequests) {
		this.context = context;
		this.registry = registry;
		this.tagsProvider = tagsProvider;
		this.metricName = metricName;
		this.autoTimeRequests = autoTimeRequests;
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
		Object handler;
		try {
			handler = getHandler(request);
		}
		catch (Exception ex) {
			logger.debug("Unable to time request", ex);
			filterChain.doFilter(request, response);
			return;
		}
		filterAndRecordMetrics(request, response, filterChain, handler);
	}

	private Object getHandler(HttpServletRequest request) throws Exception {
		HttpServletRequest wrapper = new UnmodifiableAttributesRequestWrapper(request);
		for (HandlerMapping mapping : getMappingIntrospector().getHandlerMappings()) {
			HandlerExecutionChain chain = mapping.getHandler(wrapper);
			if (chain != null) {
				if (mapping instanceof MatchableHandlerMapping) {
					return chain.getHandler();
				}
				return null;
			}
		}
		return null;
	}

	private HandlerMappingIntrospector getMappingIntrospector() {
		if (this.introspector == null) {
			this.introspector = this.context.getBean(HandlerMappingIntrospector.class);
		}
		return this.introspector;
	}

	private void filterAndRecordMetrics(HttpServletRequest request,
			HttpServletResponse response, FilterChain filterChain, Object handler)
			throws IOException, ServletException {
		TimingContext timingContext = TimingContext.get(request);
		if (timingContext == null) {
			timingContext = startAndAttachTimingContext(request, handler);
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
				record(timingContext, response, request, handler, exception);
			}
		}
		catch (NestedServletException ex) {
			response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
			record(timingContext, response, request, handler, ex.getCause());
			throw ex;
		}
	}

	private TimingContext startAndAttachTimingContext(HttpServletRequest request,
			Object handler) {
		Set<Timed> annotations = getTimedAnnotations(handler);
		Timer.Sample timerSample = Timer.start(this.registry);
		Collection<LongTaskTimer.Sample> longTaskTimerSamples = getLongTaskTimerSamples(
				request, handler, annotations);
		TimingContext timingContext = new TimingContext(annotations, timerSample,
				longTaskTimerSamples);
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
		return AnnotationUtils.getDeclaredRepeatableAnnotations(element, Timed.class);
	}

	private Collection<LongTaskTimer.Sample> getLongTaskTimerSamples(
			HttpServletRequest request, Object handler, Set<Timed> annotations) {
		List<LongTaskTimer.Sample> samples = new ArrayList<>();
		annotations.stream().filter(Timed::longTask).forEach((annotation) -> {
			Iterable<Tag> tags = this.tagsProvider.getLongRequestTags(request, handler);
			LongTaskTimer.Builder builder = LongTaskTimer.builder(annotation).tags(tags);
			LongTaskTimer timer = builder.register(this.registry);
			samples.add(timer.start());
		});
		return samples;
	}

	private void record(TimingContext timingContext, HttpServletResponse response,
			HttpServletRequest request, Object handlerObject, Throwable exception) {
		Timer.Sample timerSample = timingContext.getTimerSample();
		Supplier<Iterable<Tag>> tags = () -> this.tagsProvider.getTags(request, response,
				handlerObject, exception);
		for (Timed annotation : timingContext.getAnnotations()) {
			stop(timerSample, tags, Timer.builder(annotation, this.metricName));
		}
		if (timingContext.getAnnotations().isEmpty() && this.autoTimeRequests) {
			stop(timerSample, tags, Timer.builder(this.metricName));
		}
		for (LongTaskTimer.Sample sample : timingContext.getLongTaskTimerSamples()) {
			sample.stop();
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

		private final Set<Timed> annotations;

		private final Timer.Sample timerSample;

		private final Collection<LongTaskTimer.Sample> longTaskTimerSamples;

		TimingContext(Set<Timed> annotations, Sample timerSample,
				Collection<io.micrometer.core.instrument.LongTaskTimer.Sample> longTaskTimerSamples) {
			this.annotations = annotations;
			this.timerSample = timerSample;
			this.longTaskTimerSamples = longTaskTimerSamples;
		}

		public Set<Timed> getAnnotations() {
			return this.annotations;
		}

		public Timer.Sample getTimerSample() {
			return this.timerSample;
		}

		public Collection<LongTaskTimer.Sample> getLongTaskTimerSamples() {
			return this.longTaskTimerSamples;
		}

		public void attachTo(HttpServletRequest request) {
			request.setAttribute(ATTRIBUTE, this);
		}

		public static TimingContext get(HttpServletRequest request) {
			return (TimingContext) request.getAttribute(ATTRIBUTE);
		}

	}

	/**
	 * An {@link HttpServletRequestWrapper} that prevents modification of the request's
	 * attributes.
	 */
	private static final class UnmodifiableAttributesRequestWrapper
			extends HttpServletRequestWrapper {

		private UnmodifiableAttributesRequestWrapper(HttpServletRequest request) {
			super(request);
		}

		@Override
		public void setAttribute(String name, Object value) {
		}

	}

}
