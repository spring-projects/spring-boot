/*
 * Copyright 2012-2014 the original author or authors.
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

package org.springframework.boot.bind;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.PropertyValue;
import org.springframework.beans.PropertyValues;
import org.springframework.core.env.CompositePropertySource;
import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.core.env.PropertySource;
import org.springframework.core.env.PropertySources;
import org.springframework.core.env.PropertySourcesPropertyResolver;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.util.PatternMatchUtils;
import org.springframework.util.ReflectionUtils;
import org.springframework.validation.DataBinder;

/**
 * A {@link PropertyValues} implementation backed by a {@link PropertySources}, bridging
 * the two abstractions and allowing (for instance) a regular {@link DataBinder} to be
 * used with the latter.
 *
 * @author Dave Syer
 */
public class PropertySourcesPropertyValues implements PropertyValues {

	private final Map<String, PropertyValue> propertyValues = new ConcurrentHashMap<String, PropertyValue>();

	private final PropertySources propertySources;

	private final Collection<String> NON_ENUMERABLE_ENUMERABLES = Arrays.asList(
			StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME,
			StandardEnvironment.SYSTEM_PROPERTIES_PROPERTY_SOURCE_NAME);

	/**
	 * Create a new PropertyValues from the given PropertySources
	 * @param propertySources a PropertySources instance
	 */
	public PropertySourcesPropertyValues(PropertySources propertySources) {
		this(propertySources, null, null);
	}

	/**
	 * Create a new PropertyValues from the given PropertySources
	 * @param propertySources a PropertySources instance
	 * @param patterns property name patterns to include from system properties and
	 * environment variables
	 * @param names exact property names to include
	 */
	public PropertySourcesPropertyValues(PropertySources propertySources,
			Collection<String> patterns, Collection<String> names) {
		this.propertySources = propertySources;
		PropertySourcesPropertyResolver resolver = new PropertySourcesPropertyResolver(
				propertySources);
		String[] includes = patterns == null ? new String[0] : patterns
				.toArray(new String[0]);
		String[] exacts = names == null ? new String[0] : names.toArray(new String[0]);
		for (PropertySource<?> source : propertySources) {
			processPropertySource(source, resolver, includes, exacts);
		}
	}

	private void processPropertySource(PropertySource<?> source,
			PropertySourcesPropertyResolver resolver, String[] includes, String[] exacts) {
		if (source instanceof EnumerablePropertySource) {
			EnumerablePropertySource<?> enumerable = (EnumerablePropertySource<?>) source;
			if (enumerable.getPropertyNames().length > 0) {
				for (String propertyName : enumerable.getPropertyNames()) {
					if (this.NON_ENUMERABLE_ENUMERABLES.contains(source.getName())
							&& !PatternMatchUtils.simpleMatch(includes, propertyName)) {
						continue;
					}
					Object value = source.getProperty(propertyName);
					try {
						value = resolver.getProperty(propertyName);
					}
					catch (RuntimeException ex) {
						// Probably could not resolve placeholders, ignore it here
					}
					if (!this.propertyValues.containsKey(propertyName)) {
						this.propertyValues.put(propertyName, new PropertyValue(
								propertyName, value));
					}
				}
			}
		}
		else if (source instanceof CompositePropertySource) {
			CompositePropertySource composite = (CompositePropertySource) source;
			for (PropertySource<?> nested : extractSources(composite)) {
				processPropertySource(nested, resolver, includes, exacts);
			}
		}
		else {
			// We can only do exact matches for non-enumerable property names, but
			// that's better than nothing...
			for (String propertyName : exacts) {
				Object value;
				value = resolver.getProperty(propertyName);
				if (value != null && !this.propertyValues.containsKey(propertyName)) {
					this.propertyValues.put(propertyName, new PropertyValue(propertyName,
							value));
					continue;
				}
				value = source.getProperty(propertyName.toUpperCase());
				if (value != null && !this.propertyValues.containsKey(propertyName)) {
					this.propertyValues.put(propertyName, new PropertyValue(propertyName,
							value));
					continue;
				}
			}
		}
	}

	private Collection<PropertySource<?>> extractSources(CompositePropertySource composite) {
		Field field = ReflectionUtils.findField(CompositePropertySource.class,
				"propertySources");
		field.setAccessible(true);
		try {
			@SuppressWarnings("unchecked")
			Collection<PropertySource<?>> collection = (Collection<PropertySource<?>>) field
					.get(composite);
			return collection;
		}
		catch (Exception e) {
			throw new IllegalStateException(
					"Cannot extract property sources from composite", e);
		}
	}

	@Override
	public PropertyValue[] getPropertyValues() {
		Collection<PropertyValue> values = this.propertyValues.values();
		return values.toArray(new PropertyValue[values.size()]);
	}

	@Override
	public PropertyValue getPropertyValue(String propertyName) {
		PropertyValue propertyValue = this.propertyValues.get(propertyName);
		if (propertyValue != null) {
			return propertyValue;
		}
		for (PropertySource<?> source : this.propertySources) {
			Object value = source.getProperty(propertyName);
			if (value != null) {
				propertyValue = new PropertyValue(propertyName, value);
				this.propertyValues.put(propertyName, propertyValue);
				return propertyValue;
			}
		}
		return null;
	}

	@Override
	public PropertyValues changesSince(PropertyValues old) {
		MutablePropertyValues changes = new MutablePropertyValues();
		// for each property value in the new set
		for (PropertyValue newValue : getPropertyValues()) {
			// if there wasn't an old one, add it
			PropertyValue oldValue = old.getPropertyValue(newValue.getName());
			if (oldValue == null || !oldValue.equals(newValue)) {
				changes.addPropertyValue(newValue);
			}
		}
		return changes;
	}

	@Override
	public boolean contains(String propertyName) {
		return getPropertyValue(propertyName) != null;
	}

	@Override
	public boolean isEmpty() {
		return this.propertyValues.isEmpty();
	}

}
