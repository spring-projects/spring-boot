/*
 * Copyright 2012-2013 the original author or authors.
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

import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.PropertyValue;
import org.springframework.beans.PropertyValues;
import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.core.env.PropertySource;
import org.springframework.core.env.PropertySources;
import org.springframework.core.env.PropertySourcesPropertyResolver;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.util.PatternMatchUtils;
import org.springframework.validation.DataBinder;

/**
 * A {@link PropertyValues} implementation backed by a {@link PropertySources}, bridging
 * the two abstractions and allowing (for instance) a regular {@link DataBinder} to be
 * used with the latter.
 * 
 * @author Dave Syer
 */
public class PropertySourcesPropertyValues implements PropertyValues {

	private Map<String, PropertyValue> propertyValues = new ConcurrentHashMap<String, PropertyValue>();

	private PropertySources propertySources;

	private Collection<String> NON_ENUMERABLES = Arrays.asList(
			StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME,
			StandardEnvironment.SYSTEM_PROPERTIES_PROPERTY_SOURCE_NAME);;

	/**
	 * Create a new PropertyValues from the given PropertySources
	 * @param propertySources a PropertySources instance
	 */
	public PropertySourcesPropertyValues(PropertySources propertySources) {
		this(propertySources, null);
	}

	/**
	 * Create a new PropertyValues from the given PropertySources
	 * @param propertySources a PropertySources instance
	 * @param systemPropertyNames property names to include from system properties and
	 * environment variables
	 */
	public PropertySourcesPropertyValues(PropertySources propertySources,
			Collection<String> systemPropertyNames) {
		this.propertySources = propertySources;
		PropertySourcesPropertyResolver resolver = new PropertySourcesPropertyResolver(
				propertySources);
		String[] includes = systemPropertyNames == null ? new String[0]
				: systemPropertyNames.toArray(new String[0]);
		for (PropertySource<?> source : propertySources) {
			if (source instanceof EnumerablePropertySource) {
				EnumerablePropertySource<?> enumerable = (EnumerablePropertySource<?>) source;
				if (enumerable.getPropertyNames().length > 0) {
					for (String propertyName : enumerable.getPropertyNames()) {
						if (this.NON_ENUMERABLES.contains(source.getName())
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
						this.propertyValues.put(propertyName, new PropertyValue(
								propertyName, value));
					}
				}
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
