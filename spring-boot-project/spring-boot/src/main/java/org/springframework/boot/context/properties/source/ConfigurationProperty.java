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
public final class ConfigurationProperty
		implements OriginProvider, Comparable<ConfigurationProperty> {

	private final ConfigurationPropertyName name;

	private final Object value;

	private final Origin origin;

	public ConfigurationProperty(ConfigurationPropertyName name, Object value,
			Origin origin) {
		Assert.notNull(name, "Name must not be null");
		Assert.notNull(value, "Value must not be null");
		this.name = name;
		this.value = value;
		this.origin = origin;
	}

	public ConfigurationPropertyName getName() {
		return this.name;
	}

	public Object getValue() {
		return this.value;
	}

	@Override
	public Origin getOrigin() {
		return this.origin;
	}

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

	@Override
	public int hashCode() {
		int result = ObjectUtils.nullSafeHashCode(this.name);
		result = 31 * result + ObjectUtils.nullSafeHashCode(this.value);
		return result;
	}

	@Override
	public String toString() {
		return new ToStringCreator(this).append("name", this.name)
				.append("value", this.value).append("origin", this.origin).toString();
	}

	@Override
	public int compareTo(ConfigurationProperty other) {
		return this.name.compareTo(other.name);
	}

	static ConfigurationProperty of(ConfigurationPropertyName name,
			OriginTrackedValue value) {
		if (value == null) {
			return null;
		}
		return new ConfigurationProperty(name, value.getValue(), value.getOrigin());
	}

	static ConfigurationProperty of(ConfigurationPropertyName name, Object value,
			Origin origin) {
		if (value == null) {
			return null;
		}
		return new ConfigurationProperty(name, value, origin);
	}

}
