/*
 * Copyright 2012-2021 the original author or authors.
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

package org.springframework.boot.context.config;

import org.springframework.boot.origin.Origin;
import org.springframework.boot.origin.OriginProvider;
import org.springframework.util.StringUtils;

/**
 * A user specified location that can be {@link ConfigDataLocationResolver resolved} to
 * one or more {@link ConfigDataResource config data resources}. A
 * {@link ConfigDataLocation} is a simple wrapper around a {@link String} value. The exact
 * format of the value will depend on the underlying technology, but is usually a URL like
 * syntax consisting of a prefix and path. For example, {@code crypt:somehost/somepath}.
 * <p>
 * Locations can be mandatory or {@link #isOptional() optional}. Optional locations are
 * prefixed with {@code optional:}.
 *
 * @author Phillip Webb
 * @since 2.4.0
 */
public final class ConfigDataLocation implements OriginProvider {

	/**
	 * Prefix used to indicate that a {@link ConfigDataResource} is optional.
	 */
	public static final String OPTIONAL_PREFIX = "optional:";

	private final boolean optional;

	private final String value;

	private final Origin origin;

	private ConfigDataLocation(boolean optional, String value, Origin origin) {
		this.value = value;
		this.optional = optional;
		this.origin = origin;
	}

	/**
	 * Return the the location is optional and should ignore
	 * {@link ConfigDataNotFoundException}.
	 * @return if the location is optional
	 */
	public boolean isOptional() {
		return this.optional;
	}

	/**
	 * Return the value of the location (always excluding any user specified
	 * {@code optional:} prefix.
	 * @return the location value
	 */
	public String getValue() {
		return this.value;
	}

	/**
	 * Return if {@link #getValue()} has the specified prefix.
	 * @param prefix the prefix to check
	 * @return if the value has the prefix
	 */
	public boolean hasPrefix(String prefix) {
		return this.value.startsWith(prefix);
	}

	/**
	 * Return {@link #getValue()} with the specified prefix removed. If the location does
	 * not have the given prefix then the {@link #getValue()} is returned unchanged.
	 * @param prefix the prefix to check
	 * @return the value with the prefix removed
	 */
	public String getNonPrefixedValue(String prefix) {
		if (hasPrefix(prefix)) {
			return this.value.substring(prefix.length());
		}
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
		ConfigDataLocation other = (ConfigDataLocation) obj;
		return this.value.equals(other.value);
	}

	@Override
	public int hashCode() {
		return this.value.hashCode();
	}

	@Override
	public String toString() {
		return (!this.optional) ? this.value : OPTIONAL_PREFIX + this.value;
	}

	/**
	 * Create a new {@link ConfigDataLocation} with a specific {@link Origin}.
	 * @param origin the origin to set
	 * @return a new {@link ConfigDataLocation} instance.
	 */
	ConfigDataLocation withOrigin(Origin origin) {
		return new ConfigDataLocation(this.optional, this.value, origin);
	}

	/**
	 * Factory method to create a new {@link ConfigDataLocation} from a string.
	 * @param location the location string
	 * @return a {@link ConfigDataLocation} instance or {@code null} if no location was
	 * provided
	 */
	public static ConfigDataLocation of(String location) {
		boolean optional = location != null && location.startsWith(OPTIONAL_PREFIX);
		String value = (!optional) ? location : location.substring(OPTIONAL_PREFIX.length());
		if (!StringUtils.hasText(value)) {
			return null;
		}
		return new ConfigDataLocation(optional, value, null);
	}

}
