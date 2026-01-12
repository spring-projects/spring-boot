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

package org.springframework.boot.test.context;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jspecify.annotations.Nullable;

import org.springframework.boot.test.context.PropertyMapping.Skip;
import org.springframework.core.annotation.MergedAnnotation;
import org.springframework.core.annotation.MergedAnnotationPredicates;
import org.springframework.core.annotation.MergedAnnotations;
import org.springframework.core.annotation.MergedAnnotations.SearchStrategy;
import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.test.context.TestContextAnnotationUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

/**
 * {@link EnumerablePropertySource} to adapt annotations marked with
 * {@link PropertyMapping @PropertyMapping}.
 *
 * @author Phillip Webb
 * @author Andy Wilkinson
 */
class AnnotationsPropertySource extends EnumerablePropertySource<Class<?>> {

	private static final Pattern CAMEL_CASE_PATTERN = Pattern.compile("([^A-Z-])([A-Z])");

	private final Map<String, Object> properties;

	AnnotationsPropertySource(Class<?> source) {
		this("Annotations", source);
	}

	AnnotationsPropertySource(String name, Class<?> source) {
		super(name, source);
		this.properties = getProperties(source);
	}

	private Map<String, Object> getProperties(Class<?> source) {
		Map<String, Object> properties = new LinkedHashMap<>();
		getProperties(source, properties);
		return properties;
	}

	private void getProperties(Class<?> source, Map<String, Object> properties) {
		MergedAnnotations.from(source, SearchStrategy.SUPERCLASS)
			.stream()
			.filter(MergedAnnotationPredicates.unique(MergedAnnotation::getType))
			.forEach((annotation) -> {
				Class<Annotation> type = annotation.getType();
				MergedAnnotation<?> typeMapping = MergedAnnotations.from(type)
					.get(PropertyMapping.class, MergedAnnotation::isDirectlyPresent);
				String prefix = typeMapping.getValue(MergedAnnotation.VALUE, String.class).orElse("");
				Skip defaultSkip = typeMapping.getValue("skip", Skip.class).orElse(Skip.YES);
				for (Method attribute : type.getDeclaredMethods()) {
					collectProperties(prefix, defaultSkip, annotation, attribute, properties);
				}
			});
		if (TestContextAnnotationUtils.searchEnclosingClass(source)) {
			getProperties(source.getEnclosingClass(), properties);
		}
	}

	private void collectProperties(String prefix, Skip skip, MergedAnnotation<?> annotation, Method attribute,
			Map<String, Object> properties) {
		MergedAnnotation<?> attributeMapping = MergedAnnotations.from(attribute).get(PropertyMapping.class);
		skip = attributeMapping.getValue("skip", Skip.class).orElse(skip);
		if (skip == Skip.YES) {
			return;
		}
		Optional<Object> value = annotation.getValue(attribute.getName());
		if (value.isEmpty()) {
			return;
		}
		if (skip == Skip.ON_DEFAULT_VALUE) {
			if (ObjectUtils.nullSafeEquals(value.get(), annotation.getDefaultValue(attribute.getName()).orElse(null))) {
				return;
			}
		}
		String name = getName(prefix, attributeMapping, attribute);
		putProperties(name, skip, value.get(), properties);
	}

	private String getName(String prefix, MergedAnnotation<?> attributeMapping, Method attribute) {
		String name = attributeMapping.getValue(MergedAnnotation.VALUE, String.class).orElse("");
		if (!StringUtils.hasText(name)) {
			name = toKebabCase(attribute.getName());
		}
		return dotAppend(prefix, name);
	}

	private String toKebabCase(String name) {
		Matcher matcher = CAMEL_CASE_PATTERN.matcher(name);
		StringBuilder result = new StringBuilder();
		while (matcher.find()) {
			matcher.appendReplacement(result, matcher.group(1) + '-' + StringUtils.uncapitalize(matcher.group(2)));
		}
		matcher.appendTail(result);
		return result.toString().toLowerCase(Locale.ENGLISH);
	}

	private String dotAppend(String prefix, String postfix) {
		if (StringUtils.hasText(prefix)) {
			return prefix.endsWith(".") ? prefix + postfix : prefix + "." + postfix;
		}
		return postfix;
	}

	private void putProperties(String name, Skip defaultSkip, Object value, Map<String, Object> properties) {
		if (ObjectUtils.isArray(value)) {
			Object[] array = ObjectUtils.toObjectArray(value);
			for (int i = 0; i < array.length; i++) {
				putProperties(name + "[" + i + "]", defaultSkip, array[i], properties);
			}
		}
		else if (value instanceof MergedAnnotation<?> annotation) {
			for (Method attribute : annotation.getType().getDeclaredMethods()) {
				collectProperties(name, defaultSkip, annotation, attribute, properties);
			}
		}
		else {
			properties.put(name, value);
		}
	}

	@Override
	public boolean containsProperty(String name) {
		return this.properties.containsKey(name);
	}

	@Override
	public @Nullable Object getProperty(String name) {
		return this.properties.get(name);
	}

	@Override
	public String[] getPropertyNames() {
		return StringUtils.toStringArray(this.properties.keySet());
	}

	boolean isEmpty() {
		return this.properties.isEmpty();
	}

	@Override
	public boolean equals(@Nullable Object obj) {
		if (obj == this) {
			return true;
		}
		if (obj == null || getClass() != obj.getClass()) {
			return false;
		}
		return this.properties.equals(((AnnotationsPropertySource) obj).properties);
	}

	@Override
	public int hashCode() {
		return this.properties.hashCode();
	}

}
