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

package org.springframework.boot.test.autoconfigure.properties;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.util.ObjectUtils;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;

/**
 * {@link EnumerablePropertySource} to adapt annotations marked with
 * {@link PropertyMapping @PropertyMapping}.
 *
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @since 1.4.0
 */
public class AnnotationsPropertySource extends EnumerablePropertySource<Class<?>> {

	private static final Pattern CAMEL_CASE_PATTERN = Pattern.compile("([^A-Z-])([A-Z])");

	private final Map<String, Object> properties;

	public AnnotationsPropertySource(Class<?> source) {
		this("Annotations", source);
	}

	public AnnotationsPropertySource(String name, Class<?> source) {
		super(name, source);
		this.properties = getProperties(source);
	}

	private Map<String, Object> getProperties(Class<?> source) {
		Map<String, Object> properties = new LinkedHashMap<>();
		collectProperties(source, source, properties, new HashSet<>());
		return Collections.unmodifiableMap(properties);
	}

	private void collectProperties(Class<?> root, Class<?> source,
			Map<String, Object> properties, Set<Class<?>> seen) {
		if (source != null && seen.add(source)) {
			for (Annotation annotation : getMergedAnnotations(root, source)) {
				if (!AnnotationUtils.isInJavaLangAnnotationPackage(annotation)) {
					PropertyMapping typeMapping = annotation.annotationType()
							.getAnnotation(PropertyMapping.class);
					for (Method attribute : annotation.annotationType()
							.getDeclaredMethods()) {
						collectProperties(annotation, attribute, typeMapping, properties);
					}
					collectProperties(root, annotation.annotationType(), properties,
							seen);
				}
			}
			collectProperties(root, source.getSuperclass(), properties, seen);
		}
	}

	private List<Annotation> getMergedAnnotations(Class<?> root, Class<?> source) {
		List<Annotation> mergedAnnotations = new ArrayList<>();
		Annotation[] annotations = AnnotationUtils.getAnnotations(source);
		if (annotations != null) {
			for (Annotation annotation : annotations) {
				if (!AnnotationUtils.isInJavaLangAnnotationPackage(annotation)) {
					Annotation mergedAnnotation = findMergedAnnotation(root,
							annotation.annotationType());
					if (mergedAnnotation != null) {
						mergedAnnotations.add(mergedAnnotation);
					}
				}
			}
		}
		return mergedAnnotations;
	}

	private Annotation findMergedAnnotation(Class<?> source,
			Class<? extends Annotation> annotationType) {
		if (source == null) {
			return null;
		}
		Annotation mergedAnnotation = AnnotatedElementUtils.getMergedAnnotation(source,
				annotationType);
		return (mergedAnnotation != null) ? mergedAnnotation
				: findMergedAnnotation(source.getSuperclass(), annotationType);
	}

	private void collectProperties(Annotation annotation, Method attribute,
			PropertyMapping typeMapping, Map<String, Object> properties) {
		PropertyMapping attributeMapping = AnnotationUtils.getAnnotation(attribute,
				PropertyMapping.class);
		SkipPropertyMapping skip = getMappingType(typeMapping, attributeMapping);
		if (skip == SkipPropertyMapping.YES) {
			return;
		}
		ReflectionUtils.makeAccessible(attribute);
		Object value = ReflectionUtils.invokeMethod(attribute, annotation);
		if (skip == SkipPropertyMapping.ON_DEFAULT_VALUE) {
			Object defaultValue = AnnotationUtils.getDefaultValue(annotation,
					attribute.getName());
			if (ObjectUtils.nullSafeEquals(value, defaultValue)) {
				return;
			}
		}
		String name = getName(typeMapping, attributeMapping, attribute);
		putProperties(name, value, properties);
	}

	private SkipPropertyMapping getMappingType(PropertyMapping typeMapping,
			PropertyMapping attributeMapping) {
		if (attributeMapping != null) {
			return attributeMapping.skip();
		}
		if (typeMapping != null) {
			return typeMapping.skip();
		}
		return SkipPropertyMapping.YES;
	}

	private String getName(PropertyMapping typeMapping, PropertyMapping attributeMapping,
			Method attribute) {
		String prefix = (typeMapping != null) ? typeMapping.value() : "";
		String name = (attributeMapping != null) ? attributeMapping.value() : "";
		if (!StringUtils.hasText(name)) {
			name = toKebabCase(attribute.getName());
		}
		return dotAppend(prefix, name);
	}

	private String toKebabCase(String name) {
		Matcher matcher = CAMEL_CASE_PATTERN.matcher(name);
		StringBuffer result = new StringBuffer();
		while (matcher.find()) {
			matcher.appendReplacement(result,
					matcher.group(1) + '-' + StringUtils.uncapitalize(matcher.group(2)));
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

	private void putProperties(String name, Object value,
			Map<String, Object> properties) {
		if (ObjectUtils.isArray(value)) {
			Object[] array = ObjectUtils.toObjectArray(value);
			for (int i = 0; i < array.length; i++) {
				properties.put(name + "[" + i + "]", array[i]);
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
	public Object getProperty(String name) {
		return this.properties.get(name);
	}

	@Override
	public String[] getPropertyNames() {
		return StringUtils.toStringArray(this.properties.keySet());
	}

	public boolean isEmpty() {
		return this.properties.isEmpty();
	}

	@Override
	public boolean equals(Object obj) {
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
