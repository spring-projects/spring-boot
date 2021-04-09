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

package org.springframework.boot.actuate.metrics.web.method;

import java.lang.reflect.AnnotatedElement;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

import io.micrometer.core.annotation.Timed;

import org.springframework.core.annotation.MergedAnnotationCollectors;
import org.springframework.core.annotation.MergedAnnotations;
import org.springframework.util.ConcurrentReferenceHashMap;
import org.springframework.web.method.HandlerMethod;

/**
 * Utility used to obtain {@link Timed @Timed} annotations from a {@link HandlerMethod}.
 *
 * @author Phillip Webb
 * @since 2.5.0
 */
public final class HandlerMethodTimedAnnotations {

	private static Map<AnnotatedElement, Set<Timed>> cache = new ConcurrentReferenceHashMap<>();

	private HandlerMethodTimedAnnotations() {
	}

	public static Set<Timed> get(HandlerMethod handler) {
		Set<Timed> methodAnnotations = findTimedAnnotations(handler.getMethod());
		if (!methodAnnotations.isEmpty()) {
			return methodAnnotations;
		}
		return findTimedAnnotations(handler.getBeanType());
	}

	private static Set<Timed> findTimedAnnotations(AnnotatedElement element) {
		Set<Timed> result = cache.get(element);
		if (result != null) {
			return result;
		}
		MergedAnnotations annotations = MergedAnnotations.from(element);
		result = (!annotations.isPresent(Timed.class)) ? Collections.emptySet()
				: annotations.stream(Timed.class).collect(MergedAnnotationCollectors.toAnnotationSet());
		cache.put(element, result);
		return result;
	}

}
