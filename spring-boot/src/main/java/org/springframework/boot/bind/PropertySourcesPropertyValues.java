/*
 * Copyright 2012-2015 the original author or authors.
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

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.PropertyValue;
import org.springframework.beans.PropertyValues;
import org.springframework.core.env.CompositePropertySource;
import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.core.env.PropertySource;
import org.springframework.core.env.PropertySources;
import org.springframework.core.env.PropertySourcesPropertyResolver;
import org.springframework.util.Assert;
import org.springframework.validation.DataBinder;

/**
 * A {@link PropertyValues} implementation backed by a {@link PropertySources}, bridging
 * the two abstractions and allowing (for instance) a regular {@link DataBinder} to be
 * used with the latter.
 *
 * @author Dave Syer
 * @author Phillip Webb
 */
public class PropertySourcesPropertyValues implements PropertyValues {

	private static final Pattern COLLECTION_PROPERTY = Pattern
			.compile("\\[(\\d+)\\](\\.\\S+)?");

	private final PropertySources propertySources;

	private final Collection<String> nonEnumerableFallbackNames;

	private final PropertyNamePatternsMatcher includes;

	private final Map<String, PropertyValue> propertyValues = new LinkedHashMap<String, PropertyValue>();

	private final ConcurrentHashMap<String, PropertySource<?>> collectionOwners = new ConcurrentHashMap<String, PropertySource<?>>();

	/**
	 * Create a new PropertyValues from the given PropertySources.
	 * @param propertySources a PropertySources instance
	 */
	public PropertySourcesPropertyValues(PropertySources propertySources) {
		this(propertySources, (Collection<String>) null, PropertyNamePatternsMatcher.ALL);
	}

	/**
	 * Create a new PropertyValues from the given PropertySources.
	 * @param propertySources a PropertySources instance
	 * @param includePatterns property name patterns to include from system properties and
	 * environment variables
	 * @param nonEnumerableFallbackNames the property names to try in lieu of an
	 * {@link EnumerablePropertySource}.
	 */
	public PropertySourcesPropertyValues(PropertySources propertySources,
			Collection<String> includePatterns,
			Collection<String> nonEnumerableFallbackNames) {
		this(propertySources, nonEnumerableFallbackNames,
				new PatternPropertyNamePatternsMatcher(includePatterns));
	}

	/**
	 * Create a new PropertyValues from the given PropertySources.
	 * @param propertySources a PropertySources instance
	 * @param nonEnumerableFallbackNames the property names to try in lieu of an
	 * {@link EnumerablePropertySource}.
	 * @param includes the property name patterns to include
	 */
	PropertySourcesPropertyValues(PropertySources propertySources,
			Collection<String> nonEnumerableFallbackNames,
			PropertyNamePatternsMatcher includes) {
		Assert.notNull(propertySources, "PropertySources must not be null");
		Assert.notNull(includes, "Includes must not be null");
		this.propertySources = propertySources;
		this.nonEnumerableFallbackNames = nonEnumerableFallbackNames;
		this.includes = includes;
		PropertySourcesPropertyResolver resolver = new PropertySourcesPropertyResolver(
				propertySources);
		for (PropertySource<?> source : propertySources) {
			processPropertySource(source, resolver);
		}
	}

	private void processPropertySource(PropertySource<?> source,
			PropertySourcesPropertyResolver resolver) {
		if (source instanceof CompositePropertySource) {
			processCompositePropertySource((CompositePropertySource) source, resolver);
		}
		else if (source instanceof EnumerablePropertySource) {
			processEnumerablePropertySource((EnumerablePropertySource<?>) source,
					resolver, this.includes);
		}
		else {
			processNonEnumerablePropertySource(source, resolver);
		}
	}

	private void processCompositePropertySource(CompositePropertySource source,
			PropertySourcesPropertyResolver resolver) {
		for (PropertySource<?> nested : source.getPropertySources()) {
			processPropertySource(nested, resolver);
		}
	}

	private void processEnumerablePropertySource(EnumerablePropertySource<?> source,
			PropertySourcesPropertyResolver resolver,
			PropertyNamePatternsMatcher includes) {
		if (source.getPropertyNames().length > 0) {
			for (String propertyName : source.getPropertyNames()) {
				if (includes.matches(propertyName)) {
					Object value = getEnumerableProperty(source, resolver, propertyName);
					putIfAbsent(propertyName, value, source);
				}
			}
		}
	}

	private Object getEnumerableProperty(EnumerablePropertySource<?> source,
			PropertySourcesPropertyResolver resolver, String propertyName) {
		try {
			return resolver.getProperty(propertyName, Object.class);
		}
		catch (RuntimeException ex) {
			// Probably could not resolve placeholders, ignore it here
			return source.getProperty(propertyName);
		}
	}

	private void processNonEnumerablePropertySource(PropertySource<?> source,
			PropertySourcesPropertyResolver resolver) {
		// We can only do exact matches for non-enumerable property names, but
		// that's better than nothing...
		if (this.nonEnumerableFallbackNames == null) {
			return;
		}
		for (String propertyName : this.nonEnumerableFallbackNames) {
			if (!source.containsProperty(propertyName)) {
				continue;
			}
			Object value = null;
			try {
				value = resolver.getProperty(propertyName, Object.class);
			}
			catch (RuntimeException ex) {
				// Probably could not convert to Object, weird, but ignorable
			}
			if (value == null) {
				value = source.getProperty(propertyName.toUpperCase());
			}
			putIfAbsent(propertyName, value, source);
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
			propertyValue = putIfAbsent(propertyName, value, source);
			if (propertyValue != null) {
				return propertyValue;
			}
		}
		return null;
	}

	private PropertyValue putIfAbsent(String propertyName, Object value,
			PropertySource<?> source) {
		if (value != null && !this.propertyValues.containsKey(propertyName)) {
			PropertySource<?> collectionOwner = this.collectionOwners.putIfAbsent(
					COLLECTION_PROPERTY.matcher(propertyName).replaceAll("[]"), source);
			if (collectionOwner == null || collectionOwner == source) {
				PropertyValue propertyValue = new OriginCapablePropertyValue(propertyName,
						value, propertyName, source);
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
