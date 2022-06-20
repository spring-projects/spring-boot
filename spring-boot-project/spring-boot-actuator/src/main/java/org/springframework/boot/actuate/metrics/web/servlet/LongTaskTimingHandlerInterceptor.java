/*
 * Copyright 2012-2022 the original author or authors.
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

import java.lang.reflect.AnnotatedElement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import io.micrometer.core.annotation.Timed;
import io.micrometer.core.instrument.LongTaskTimer;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.core.annotation.MergedAnnotationCollectors;
import org.springframework.core.annotation.MergedAnnotations;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * A {@link HandlerInterceptor} that supports Micrometer's long task timers configured on
 * a handler using {@link Timed @Timed} with {@link Timed#longTask() longTask} set to
 * {@code true}.
 *
 * @author Andy Wilkinson
 * @since 2.0.7
 */
public class LongTaskTimingHandlerInterceptor implements HandlerInterceptor {

	private static final Log logger = LogFactory.getLog(LongTaskTimingHandlerInterceptor.class);

	private final MeterRegistry registry;

	private final WebMvcTagsProvider tagsProvider;

	/**
	 * Creates a new {@code LongTaskTimingHandlerInterceptor} that will create
	 * {@link LongTaskTimer LongTaskTimers} using the given registry. Timers will be
	 * tagged using the given {@code tagsProvider}.
	 * @param registry the registry
	 * @param tagsProvider the tags provider
	 */
	public LongTaskTimingHandlerInterceptor(MeterRegistry registry, WebMvcTagsProvider tagsProvider) {
		this.registry = registry;
		this.tagsProvider = tagsProvider;
	}

	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
			throws Exception {
		LongTaskTimingContext timingContext = LongTaskTimingContext.get(request);
		if (timingContext == null) {
			startAndAttachTimingContext(request, handler);
		}
		return true;
	}

	@Override
	public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex)
			throws Exception {
		if (!request.isAsyncStarted()) {
			stopLongTaskTimers(LongTaskTimingContext.get(request));
		}
	}

	private void startAndAttachTimingContext(HttpServletRequest request, Object handler) {
		Set<Timed> annotations = getTimedAnnotations(handler);
		Collection<LongTaskTimer.Sample> longTaskTimerSamples = getLongTaskTimerSamples(request, handler, annotations);
		LongTaskTimingContext timingContext = new LongTaskTimingContext(longTaskTimerSamples);
		timingContext.attachTo(request);
	}

	private Collection<LongTaskTimer.Sample> getLongTaskTimerSamples(HttpServletRequest request, Object handler,
			Set<Timed> annotations) {
		List<LongTaskTimer.Sample> samples = new ArrayList<>();
		try {
			annotations.stream().filter(Timed::longTask).forEach((annotation) -> {
				Iterable<Tag> tags = this.tagsProvider.getLongRequestTags(request, handler);
				LongTaskTimer.Builder builder = LongTaskTimer.builder(annotation).tags(tags);
				LongTaskTimer timer = builder.register(this.registry);
				samples.add(timer.start());
			});
		}
		catch (Exception ex) {
			logger.warn("Failed to start long task timers", ex);
			// Allow request-response exchange to continue, unaffected by metrics problem
		}
		return samples;
	}

	private Set<Timed> getTimedAnnotations(Object handler) {
		if (handler instanceof HandlerMethod handlerMethod) {
			return getTimedAnnotations(handlerMethod);
		}
		return Collections.emptySet();
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

	private void stopLongTaskTimers(LongTaskTimingContext timingContext) {
		for (LongTaskTimer.Sample sample : timingContext.getLongTaskTimerSamples()) {
			sample.stop();
		}
	}

	/**
	 * Context object attached to a request to retain information across the multiple
	 * interceptor calls that happen with async requests.
	 */
	static class LongTaskTimingContext {

		private static final String ATTRIBUTE = LongTaskTimingContext.class.getName();

		private final Collection<LongTaskTimer.Sample> longTaskTimerSamples;

		LongTaskTimingContext(Collection<LongTaskTimer.Sample> longTaskTimerSamples) {
			this.longTaskTimerSamples = longTaskTimerSamples;
		}

		Collection<LongTaskTimer.Sample> getLongTaskTimerSamples() {
			return this.longTaskTimerSamples;
		}

		void attachTo(HttpServletRequest request) {
			request.setAttribute(ATTRIBUTE, this);
		}

		static LongTaskTimingContext get(HttpServletRequest request) {
			return (LongTaskTimingContext) request.getAttribute(ATTRIBUTE);
		}

	}

}
