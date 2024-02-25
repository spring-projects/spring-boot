/*
 * Copyright 2012-2023 the original author or authors.
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

package org.springframework.boot.test.autoconfigure.properties;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
 * @since 1.4.0
 */
public class AnnotationsPropertySource extends EnumerablePropertySource<Class<?>> {

	private static final Pattern CAMEL_CASE_PATTERN = Pattern.compile("([^A-Z-])([A-Z])");

	private final Map<String, Object> properties;

	/**
	 * Constructs a new AnnotationsPropertySource with the specified source class.
	 * @param source the source class for the property source
	 */
	public AnnotationsPropertySource(Class<?> source) {
		this("Annotations", source);
	}

	/**
	 * Constructs a new AnnotationsPropertySource with the specified name and source
	 * class.
	 * @param name the name of the property source
	 * @param source the source class from which to retrieve the properties
	 */
	public AnnotationsPropertySource(String name, Class<?> source) {
		super(name, source);
		this.properties = getProperties(source);
	}

	/**
	 * Retrieves the properties of the given source class and returns them as a map. The
	 * properties are obtained recursively from the source class and its superclasses.
	 * @param source the class from which to retrieve the properties
	 * @return a map containing the properties of the source class
	 */
	private Map<String, Object> getProperties(Class<?> source) {
		Map<String, Object> properties = new LinkedHashMap<>();
		getProperties(source, properties);
		return properties;
	}

	/**
	 * Retrieves properties from the given source class and adds them to the provided
	 * properties map.
	 * @param source the source class from which to retrieve properties
	 * @param properties the map to which the retrieved properties will be added
	 */
	private void getProperties(Class<?> source, Map<String, Object> properties) {
		MergedAnnotations.from(source, SearchStrategy.SUPERCLASS)
			.stream()
			.filter(MergedAnnotationPredicates.unique(MergedAnnotation::getType))
			.forEach((annotation) -> {
				Class<Annotation> type = annotation.getType();
				MergedAnnotation<?> typeMapping = MergedAnnotations.from(type)
					.get(PropertyMapping.class, MergedAnnotation::isDirectlyPresent);
				String prefix = typeMapping.getValue(MergedAnnotation.VALUE, String.class).orElse("");
				SkipPropertyMapping defaultSkip = typeMapping.getValue("skip", SkipPropertyMapping.class)
					.orElse(SkipPropertyMapping.YES);
				for (Method attribute : type.getDeclaredMethods()) {
					collectProperties(prefix, defaultSkip, annotation, attribute, properties);
				}
			});
		if (TestContextAnnotationUtils.searchEnclosingClass(source)) {
			getProperties(source.getEnclosingClass(), properties);
		}
	}

	/**
	 * Collects properties from the given annotation and adds them to the properties map.
	 * @param prefix the prefix to be added to the property names
	 * @param skip the skip property mapping option
	 * @param annotation the merged annotation containing the properties
	 * @param attribute the method representing the attribute
	 * @param properties the map to store the collected properties
	 */
	private void collectProperties(String prefix, SkipPropertyMapping skip, MergedAnnotation<?> annotation,
			Method attribute, Map<String, Object> properties) {
		MergedAnnotation<?> attributeMapping = MergedAnnotations.from(attribute).get(PropertyMapping.class);
		skip = attributeMapping.getValue("skip", SkipPropertyMapping.class).orElse(skip);
		if (skip == SkipPropertyMapping.YES) {
			return;
		}
		Optional<Object> value = annotation.getValue(attribute.getName());
		if (value.isEmpty()) {
			return;
		}
		if (skip == SkipPropertyMapping.ON_DEFAULT_VALUE) {
			if (ObjectUtils.nullSafeEquals(value.get(), annotation.getDefaultValue(attribute.getName()).orElse(null))) {
				return;
			}
		}
		String name = getName(prefix, attributeMapping, attribute);
		putProperties(name, skip, value.get(), properties);
	}

	/**
	 * Returns the name of the attribute.
	 * @param prefix the prefix to be appended to the name
	 * @param attributeMapping the merged annotation containing the attribute mapping
	 * @param attribute the method representing the attribute
	 * @return the name of the attribute
	 */
	private String getName(String prefix, MergedAnnotation<?> attributeMapping, Method attribute) {
		String name = attributeMapping.getValue(MergedAnnotation.VALUE, String.class).orElse("");
		if (!StringUtils.hasText(name)) {
			name = toKebabCase(attribute.getName());
		}
		return dotAppend(prefix, name);
	}

	/**
	 * Converts a given string to kebab case.
	 * @param name the string to be converted
	 * @return the kebab case representation of the given string
	 */
	private String toKebabCase(String name) {
		Matcher matcher = CAMEL_CASE_PATTERN.matcher(name);
		StringBuilder result = new StringBuilder();
		while (matcher.find()) {
			matcher.appendReplacement(result, matcher.group(1) + '-' + StringUtils.uncapitalize(matcher.group(2)));
		}
		matcher.appendTail(result);
		return result.toString().toLowerCase(Locale.ENGLISH);
	}

	/**
	 * Appends the given postfix to the given prefix, ensuring that a dot is added between
	 * them if necessary.
	 * @param prefix the prefix string
	 * @param postfix the postfix string
	 * @return the resulting string with the postfix appended to the prefix, with a dot
	 * added if necessary
	 */
	private String dotAppend(String prefix, String postfix) {
		if (StringUtils.hasText(prefix)) {
			return prefix.endsWith(".") ? prefix + postfix : prefix + "." + postfix;
		}
		return postfix;
	}

	/**
	 * Puts the properties into the given map.
	 * @param name the name of the property
	 * @param defaultSkip the default skip property mapping
	 * @param value the value of the property
	 * @param properties the map to put the properties into
	 */
	private void putProperties(String name, SkipPropertyMapping defaultSkip, Object value,
			Map<String, Object> properties) {
		if (ObjectUtils.isArray(value)) {
			Object[] array = ObjectUtils.toObjectArray(value);
			for (int i = 0; i < array.length; i++) {
				putProperties(name + "[" + i + "]", defaultSkip, array[i], properties);
			}
		}
		else if (value instanceof MergedAnnotation<?> annotation) {
			for (Method attribute : annotation.getType().getDeclaredMethods()) {
				collectProperties(name, defaultSkip, (MergedAnnotation<?>) value, attribute, properties);
			}
		}
		else {
			properties.put(name, value);
		}
	}

	/**
	 * Returns true if the specified property name is contained in the properties map.
	 * @param name the name of the property to check
	 * @return true if the property is contained in the map, false otherwise
	 */
	@Override
	public boolean containsProperty(String name) {
		return this.properties.containsKey(name);
	}

	/**
	 * Retrieves the value of the specified property.
	 * @param name the name of the property to retrieve
	 * @return the value of the property, or null if the property does not exist
	 */
	@Override
	public Object getProperty(String name) {
		return this.properties.get(name);
	}

	/**
	 * Returns an array of property names.
	 * @return an array of property names
	 */
	@Override
	public String[] getPropertyNames() {
		return StringUtils.toStringArray(this.properties.keySet());
	}

	/**
	 * Returns true if the properties map is empty.
	 * @return true if the properties map is empty, false otherwise
	 */
	public boolean isEmpty() {
		return this.properties.isEmpty();
	}

	/**
	 * Compares this AnnotationsPropertySource object to the specified object for
	 * equality.
	 * @param obj the object to compare to
	 * @return true if the objects are equal, false otherwise
	 */
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

	/**
	 * Returns the hash code value for this AnnotationsPropertySource object.
	 * @return the hash code value for this AnnotationsPropertySource object
	 */
	@Override
	public int hashCode() {
		return this.properties.hashCode();
	}

}
