/*
 * Copyright 2012-2017 the original author or authors.
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

package org.springframework.boot.context.properties;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.boot.context.properties.source.ConfigurationPropertyName;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;

/**
 * Utility class that reads
 * {@link org.springframework.boot.context.properties.ConfigurationPropertyValue}
 * annotations from getters and fields of a bean and provides methods to access the
 * metadata contained in the annotations.
 *
 * @author Tom Hombergs
 * @since 2.0.0
 * @see org.springframework.boot.context.properties.ConfigurationPropertyValue
 */
public class ConfigurationPropertyValueReader {

	private Map<ConfigurationPropertyName, ConfigurationPropertyValue> annotations = new HashMap<>();

	public ConfigurationPropertyValueReader(Object bean, String prefix) {
		this.annotations = findAnnotations(bean, prefix);
	}

	/**
	 * Returns a map that maps configuration property names to their fallback properties.
	 * If this map does not contain a value for a certain configuration property, it means
	 * that there is no fallback specified for this property.
	 *
	 * @return a map of configuration property names to their fallback property names.
	 */
	public Map<ConfigurationPropertyName, ConfigurationPropertyName> getPropertyFallbacks() {
		return this.annotations.entrySet().stream()
				.filter(entry -> !StringUtils.isEmpty(entry.getValue().fallback()))
				.collect(Collectors.toMap(Map.Entry::getKey,
						entry -> ConfigurationPropertyName
								.of(entry.getValue().fallback())));
	}

	/**
	 * Walks through the methods and fields of the specified bean to extract
	 * {@link ConfigurationPropertyValue} annotations. A field can be annotated either by
	 * directly annotating the field or by annotating the corresponding getter or setter
	 * method. This method will throw a {@link BeanCreationException} if multiple
	 * annotations are found for the same field.
	 *
	 * @param bean the bean whose annotations to retrieve.
	 * @param prefix the prefix of the superordinate {@link ConfigurationProperties}
	 * annotation. May be null.
	 * @return a map that maps configuration property names to the annotations that were
	 * found for them.
	 * @throws ConfigurationPropertyValueBindingException if multiple
	 * {@link ConfigurationPropertyValue} annotations have been found for the same field.
	 */
	private Map<ConfigurationPropertyName, ConfigurationPropertyValue> findAnnotations(
			Object bean, String prefix) {
		Map<ConfigurationPropertyName, ConfigurationPropertyValue> fieldAnnotations = new HashMap<>();
		ReflectionUtils.doWithMethods(bean.getClass(), method -> {
			ConfigurationPropertyValue annotation = AnnotationUtils.findAnnotation(method,
					ConfigurationPropertyValue.class);
			if (annotation != null) {
				PropertyDescriptor propertyDescriptor = findPropertyDescriptorOrFail(
						method);
				ConfigurationPropertyName name = getConfigurationPropertyName(prefix,
						propertyDescriptor.getName());
				if (fieldAnnotations.containsKey(name)) {
					throw ConfigurationPropertyValueBindingException.invalidUseOnMethod(
							bean.getClass(), method.getName(),
							"You may either annotate a field, a getter or a setter but not two of these.");
				}
				fieldAnnotations.put(name, annotation);
			}
		});

		ReflectionUtils.doWithFields(bean.getClass(), field -> {
			ConfigurationPropertyValue annotation = AnnotationUtils.findAnnotation(field,
					ConfigurationPropertyValue.class);
			if (annotation != null) {
				ConfigurationPropertyName name = getConfigurationPropertyName(prefix,
						field.getName());
				if (fieldAnnotations.containsKey(name)) {
					throw ConfigurationPropertyValueBindingException.invalidUseOnField(
							bean.getClass(), field.getName(),
							"You may either annotate a field, a getter or a setter but not two of these.");
				}
				fieldAnnotations.put(name, annotation);
			}
		});
		return fieldAnnotations;
	}

	private PropertyDescriptor findPropertyDescriptorOrFail(Method method) {
		PropertyDescriptor propertyDescriptor = BeanUtils.findPropertyForMethod(method);
		if (propertyDescriptor == null) {
			throw ConfigurationPropertyValueBindingException.invalidUseOnMethod(
					method.getDeclaringClass(), method.getName(),
					"This annotation may only be used on getter and setter methods or fields.");
		}
		return propertyDescriptor;
	}

	private ConfigurationPropertyName getConfigurationPropertyName(String prefix,
			String fieldName) {
		if (StringUtils.isEmpty(prefix)) {
			return ConfigurationPropertyName.of(fieldName);
		}
		else {
			return ConfigurationPropertyName.of(prefix + "." + fieldName);
		}
	}

}
