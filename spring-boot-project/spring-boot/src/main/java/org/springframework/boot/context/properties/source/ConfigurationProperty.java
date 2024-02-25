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

package org.springframework.boot.context.properties.source;

import org.springframework.boot.origin.Origin;
import org.springframework.boot.origin.OriginProvider;
import org.springframework.boot.origin.OriginTrackedValue;
import org.springframework.core.style.ToStringCreator;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;

/**
 * A single configuration property obtained from a {@link ConfigurationPropertySource}
 * consisting of a {@link #getName() name}, {@link #getValue() value} and optional
 * {@link #getOrigin() origin}.
 *
 * @author Phillip Webb
 * @author Madhura Bhave
 * @since 2.0.0
 */
public final class ConfigurationProperty implements OriginProvider, Comparable<ConfigurationProperty> {

	private final ConfigurationPropertyName name;

	private final Object value;

	private final ConfigurationPropertySource source;

	private final Origin origin;

	/**
     * Constructs a new ConfigurationProperty with the specified name, value, and origin.
     * 
     * @param name the name of the configuration property
     * @param value the value of the configuration property
     * @param origin the origin of the configuration property
     */
    public ConfigurationProperty(ConfigurationPropertyName name, Object value, Origin origin) {
		this(null, name, value, origin);
	}

	/**
     * Constructs a new ConfigurationProperty with the given source, name, value, and origin.
     * 
     * @param source the source of the configuration property
     * @param name the name of the configuration property
     * @param value the value of the configuration property
     * @param origin the origin of the configuration property
     * @throws IllegalArgumentException if the name or value is null
     */
    private ConfigurationProperty(ConfigurationPropertySource source, ConfigurationPropertyName name, Object value,
			Origin origin) {
		Assert.notNull(name, "Name must not be null");
		Assert.notNull(value, "Value must not be null");
		this.source = source;
		this.name = name;
		this.value = value;
		this.origin = origin;
	}

	/**
	 * Return the {@link ConfigurationPropertySource} that provided the property or
	 * {@code null} if the source is unknown.
	 * @return the configuration property source
	 * @since 2.6.0
	 */
	public ConfigurationPropertySource getSource() {
		return this.source;
	}

	/**
	 * Return the name of the configuration property.
	 * @return the configuration property name
	 */
	public ConfigurationPropertyName getName() {
		return this.name;
	}

	/**
	 * Return the value of the configuration property.
	 * @return the configuration property value
	 */
	public Object getValue() {
		return this.value;
	}

	/**
     * Returns the origin of the configuration property.
     *
     * @return the origin of the configuration property
     */
    @Override
	public Origin getOrigin() {
		return this.origin;
	}

	/**
     * Compares this ConfigurationProperty object to the specified object for equality.
     * Returns true if the objects are equal, false otherwise.
     * 
     * @param obj the object to compare to
     * @return true if the objects are equal, false otherwise
     */
    @Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null || getClass() != obj.getClass()) {
			return false;
		}
		ConfigurationProperty other = (ConfigurationProperty) obj;
		boolean result = true;
		result = result && ObjectUtils.nullSafeEquals(this.name, other.name);
		result = result && ObjectUtils.nullSafeEquals(this.value, other.value);
		return result;
	}

	/**
     * Returns a hash code value for the object. This method overrides the default implementation of the hashCode() method.
     * The hash code is calculated based on the name and value of the ConfigurationProperty object.
     *
     * @return the hash code value for the object
     */
    @Override
	public int hashCode() {
		int result = ObjectUtils.nullSafeHashCode(this.name);
		result = 31 * result + ObjectUtils.nullSafeHashCode(this.value);
		return result;
	}

	/**
     * Returns a string representation of the object.
     * 
     * @return a string representation of the object
     */
    @Override
	public String toString() {
		return new ToStringCreator(this).append("name", this.name)
			.append("value", this.value)
			.append("origin", this.origin)
			.toString();
	}

	/**
     * Compares this ConfigurationProperty object with the specified ConfigurationProperty object for order.
     * 
     * @param other the ConfigurationProperty object to be compared
     * @return a negative integer, zero, or a positive integer as this ConfigurationProperty is less than, equal to, or greater than the specified ConfigurationProperty
     */
    @Override
	public int compareTo(ConfigurationProperty other) {
		return this.name.compareTo(other.name);
	}

	/**
     * Creates a new ConfigurationProperty object with the specified name and value.
     * 
     * @param name the name of the configuration property
     * @param value the value of the configuration property
     * @return a new ConfigurationProperty object
     */
    static ConfigurationProperty of(ConfigurationPropertyName name, OriginTrackedValue value) {
		if (value == null) {
			return null;
		}
		return new ConfigurationProperty(name, value.getValue(), value.getOrigin());
	}

	/**
     * Creates a new ConfigurationProperty object with the given source, name, value, and origin.
     * 
     * @param source the source of the configuration property
     * @param name the name of the configuration property
     * @param value the value of the configuration property
     * @param origin the origin of the configuration property
     * @return a new ConfigurationProperty object
     */
    static ConfigurationProperty of(ConfigurationPropertySource source, ConfigurationPropertyName name, Object value,
			Origin origin) {
		if (value == null) {
			return null;
		}
		return new ConfigurationProperty(source, name, value, origin);
	}

}
