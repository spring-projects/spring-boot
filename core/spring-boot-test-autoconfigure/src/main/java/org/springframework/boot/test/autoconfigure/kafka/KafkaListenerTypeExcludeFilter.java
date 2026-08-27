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

package org.springframework.boot.test.autoconfigure.kafka;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

import org.springframework.boot.test.context.filter.annotation.AnnotationCustomizableTypeExcludeFilter;
import org.springframework.context.annotation.ComponentScan.Filter;
import org.springframework.core.annotation.MergedAnnotations;

public class KafkaListenerTypeExcludeFilter extends AnnotationCustomizableTypeExcludeFilter {

	private final KafkaListenerTest annotation;

	KafkaListenerTypeExcludeFilter(Class<?> testClass) {
		this.annotation = MergedAnnotations.from(testClass, MergedAnnotations.SearchStrategy.TYPE_HIERARCHY)
			.get(KafkaListenerTest.class)
			.synthesize();
	}

	@Override
	protected boolean hasAnnotation() {
		return true;
	}

	@Override
	protected Filter[] getFilters(FilterType type) {
		return switch (type) {
			case INCLUDE -> this.annotation.includeFilters();
			case EXCLUDE -> this.annotation.excludeFilters();
		};
	}

	@Override
	protected boolean isUseDefaultFilters() {
		return this.annotation.useDefaultFilters();
	}

	@Override
	protected Set<Class<?>> getDefaultIncludes() {
		if (this.annotation.listeners().length == 0) {
			return Collections.emptySet();
		}
		return new LinkedHashSet<>(Arrays.asList(this.annotation.listeners()));
	}

	@Override
	protected Set<Class<?>> getComponentIncludes() {
		return Collections.emptySet();
	}

}
