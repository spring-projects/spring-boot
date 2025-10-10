/*
 * Copyright 2012-present the original author or authors.
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

import org.jspecify.annotations.Nullable;

import org.springframework.boot.origin.Origin;
import org.springframework.boot.origin.OriginProvider;
import org.springframework.util.Assert;
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

	private static final ConfigDataLocation EMPTY = new ConfigDataLocation(false, "", null);

	/**
	 * Prefix used to indicate that a {@link ConfigDataResource} is optional.
	 */
	public static final String OPTIONAL_PREFIX = "optional:";

	private final boolean optional;

	private final String value;

	private final @Nullable Origin origin;

	private ConfigDataLocation(boolean optional, String value, @Nullable Origin origin) {
		this.value = value;
		this.optional = optional;
		this.origin = origin;
	}

	/**
	 * Return if the location is optional and should ignore
	 * {@link ConfigDataNotFoundException}.
	 * @return if the location is optional
	 */
	public boolean isOptional() {
		return this.optional;
	}

	/**
	 * Return the value of the location (always excluding any user specified
	 * {@code optional:} prefix).
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
		return (!hasPrefix(prefix)) ? this.value : this.value.substring(prefix.length());
	}

	@Override
	public @Nullable Origin getOrigin() {
		return this.origin;
	}

	/**
	 * Return an array of {@link ConfigDataLocation} elements built by splitting this
	 * {@link ConfigDataLocation} around a delimiter of {@code ";"}.
	 * @return the split locations
	 * @since 2.4.7
	 */
	public ConfigDataLocation[] split() {
		return split(";");
	}

	/**
	 * Return an array of {@link ConfigDataLocation} elements built by splitting this
	 * {@link ConfigDataLocation} around the specified delimiter.
	 * @param delimiter the delimiter to split on
	 * @return the split locations
	 * @since 2.4.7
	 */
	public ConfigDataLocation[] split(String delimiter) {
		Assert.state(!this.value.isEmpty(), "Unable to split empty locations");
		String[] values = StringUtils.delimitedListToStringArray(toString(), delimiter);
		ConfigDataLocation[] result = new ConfigDataLocation[values.length];
		for (int i = 0; i < values.length; i++) {
			int index = i;
			ConfigDataLocation configDataLocation = of(values[index]);
			result[i] = configDataLocation.withOrigin(getOrigin());
		}
		return result;
	}

	/**
	 * Create a new {@link ConfigDataLocation} with a specific {@link Origin}.
	 * @param origin the origin to set
	 * @return a new {@link ConfigDataLocation} instance.
	 */
	ConfigDataLocation withOrigin(@Nullable Origin origin) {
		return new ConfigDataLocation(this.optional, this.value, origin);
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
	 * Factory method to create a new {@link ConfigDataLocation} from a string.
	 * @param location the location string
	 * @return the {@link ConfigDataLocation} (which may be empty)
	 */
	public static ConfigDataLocation of(@Nullable String location) {
		boolean optional = location != null && location.startsWith(OPTIONAL_PREFIX);
		String value = (location != null && optional) ? location.substring(OPTIONAL_PREFIX.length()) : location;
		return (StringUtils.hasText(value)) ? new ConfigDataLocation(optional, value, null) : EMPTY;
	}

	static boolean isNotEmpty(@Nullable ConfigDataLocation location) {
		return (location != null) && !location.getValue().isEmpty();
	}

}
