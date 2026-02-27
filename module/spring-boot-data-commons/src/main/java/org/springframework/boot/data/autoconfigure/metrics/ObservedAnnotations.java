/*
 * Copyright 2012-present the original author or authors.
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

package org.springframework.boot.data.autoconfigure.metrics;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.util.Map;

import io.micrometer.observation.annotation.Observed;
import org.jspecify.annotations.Nullable;

import org.springframework.core.annotation.MergedAnnotations;
import org.springframework.util.ConcurrentReferenceHashMap;

/**
 * Utility used to obtain {@link Observed @Observed} annotations from repository methods
 * or types.
 *
 * @author Kwonneung Lee
 */
final class ObservedAnnotations {

	private static final Map<AnnotatedElement, Observed> cache = new ConcurrentReferenceHashMap<>();

	private ObservedAnnotations() {
	}

	/**
	 * Return the {@link Observed} annotation that should be used for the given
	 * {@code method} and {@code type}.
	 * @param method the source method
	 * @param type the source type
	 * @return the {@link Observed} annotation to use or {@code null}
	 */
	static @Nullable Observed get(Method method, Class<?> type) {
		Observed methodAnnotation = findObservedAnnotation(method);
		return (methodAnnotation != null) ? methodAnnotation : findObservedAnnotation(type);
	}

	private static @Nullable Observed findObservedAnnotation(AnnotatedElement element) {
		Observed result = cache.get(element);
		if (result != null) {
			return result;
		}
		MergedAnnotations annotations = MergedAnnotations.from(element);
		if (!annotations.isPresent(Observed.class)) {
			return null;
		}
		result = annotations.get(Observed.class).synthesize();
		cache.put(element, result);
		return result;
	}
}
