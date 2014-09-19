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

	private static final Collection<String> NON_ENUMERABLE_ENUMERABLES = Arrays.asList(
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
		String[] includes = toArray(patterns);
		String[] exacts = toArray(names);
		for (PropertySource<?> source : propertySources) {
			processPropertySource(source, resolver, includes, exacts);
		}
	}

	private String[] toArray(Collection<String> strings) {
		if (strings == null) {
			return new String[0];
		}
		return strings.toArray(new String[strings.size()]);
	}

	private void processPropertySource(PropertySource<?> source,
			PropertySourcesPropertyResolver resolver, String[] includes, String[] exacts) {
		if (source instanceof EnumerablePropertySource) {
			processEnumerablePropertySource((EnumerablePropertySource<?>) source,
					resolver, includes, exacts);
		}
		else if (source instanceof CompositePropertySource) {
			processCompositePropertySource((CompositePropertySource) source, resolver,
					includes, exacts);
		}
		else {
			// We can only do exact matches for non-enumerable property names, but
			// that's better than nothing...
			processDefaultPropertySource(source, resolver, includes, exacts);
		}
	}

	private void processEnumerablePropertySource(EnumerablePropertySource<?> source,
			PropertySourcesPropertyResolver resolver, String[] includes, String[] exacts) {
		if (source.getPropertyNames().length > 0) {
			for (String propertyName : source.getPropertyNames()) {
				if (PropertySourcesPropertyValues.NON_ENUMERABLE_ENUMERABLES.contains(source.getName())
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
					this.propertyValues.put(propertyName, new PropertyValue(propertyName,
							value));
				}
			}
		}
	}

	private void processCompositePropertySource(CompositePropertySource source,
			PropertySourcesPropertyResolver resolver, String[] includes, String[] exacts) {
		for (PropertySource<?> nested : extractSources(source)) {
			processPropertySource(nested, resolver, includes, exacts);
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
		catch (Exception ex) {
			throw new IllegalStateException(
					"Cannot extract property sources from composite", ex);
		}
	}

	private void processDefaultPropertySource(PropertySource<?> source,
			PropertySourcesPropertyResolver resolver, String[] includes, String[] exacts) {
		for (String propertyName : exacts) {
			Object value = null;
			try {
				value = resolver.getProperty(propertyName, Object.class);
			}
			catch (RuntimeException ex) {
				// Probably could not convert to Object, weird, but ignoreable
			}
			if (value == null) {
				value = source.getProperty(propertyName.toUpperCase());
			}
			if (value != null && !this.propertyValues.containsKey(propertyName)) {
				this.propertyValues.put(propertyName, new PropertyValue(propertyName,
						value));
				continue;
			}
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
