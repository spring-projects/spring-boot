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

package org.springframework.boot.configurationprocessor.metadata;

/**
 * Describe an item deprecation.
 *
 * @author Stephane Nicoll
 * @author Scott Frederick
 * @since 1.3.0
 */
public class ItemDeprecation {

	private String reason;

	private String replacement;

	private String since;

	private String level;

	/**
	 * Constructs a new ItemDeprecation with no parameters.
	 */
	public ItemDeprecation() {
		this(null, null, null);
	}

	/**
	 * Constructs a new ItemDeprecation with the specified reason, replacement, and since
	 * date.
	 * @param reason the reason for deprecation
	 * @param replacement the recommended replacement for the deprecated item
	 * @param since the version or date since when the item has been deprecated
	 */
	public ItemDeprecation(String reason, String replacement, String since) {
		this(reason, replacement, since, null);
	}

	/**
	 * Creates a new ItemDeprecation object with the specified reason, replacement, since,
	 * and level.
	 * @param reason the reason for deprecating the item
	 * @param replacement the recommended replacement for the deprecated item
	 * @param since the version or date since when the item has been deprecated
	 * @param level the deprecation level of the item (e.g. warning, error)
	 */
	public ItemDeprecation(String reason, String replacement, String since, String level) {
		this.reason = reason;
		this.replacement = replacement;
		this.since = since;
		this.level = level;
	}

	/**
	 * Returns the reason for the item deprecation.
	 * @return the reason for the item deprecation
	 */
	public String getReason() {
		return this.reason;
	}

	/**
	 * Sets the reason for deprecating the item.
	 * @param reason the reason for deprecating the item
	 */
	public void setReason(String reason) {
		this.reason = reason;
	}

	/**
	 * Returns the replacement value for the item.
	 * @return the replacement value for the item
	 */
	public String getReplacement() {
		return this.replacement;
	}

	/**
	 * Sets the replacement value for the item.
	 * @param replacement the replacement value to be set
	 */
	public void setReplacement(String replacement) {
		this.replacement = replacement;
	}

	/**
	 * Returns the value of the "since" field.
	 * @return the value of the "since" field
	 */
	public String getSince() {
		return this.since;
	}

	/**
	 * Sets the value of the "since" field in the ItemDeprecation class.
	 * @param since the value to set for the "since" field
	 */
	public void setSince(String since) {
		this.since = since;
	}

	/**
	 * Returns the level of item deprecation.
	 * @return the level of item deprecation
	 */
	public String getLevel() {
		return this.level;
	}

	/**
	 * Sets the level of item depreciation.
	 * @param level the level of item depreciation to be set
	 */
	public void setLevel(String level) {
		this.level = level;
	}

	/**
	 * Compares this ItemDeprecation object to the specified object for equality.
	 * @param o the object to compare to
	 * @return true if the objects are equal, false otherwise
	 */
	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		ItemDeprecation other = (ItemDeprecation) o;
		return nullSafeEquals(this.reason, other.reason) && nullSafeEquals(this.replacement, other.replacement)
				&& nullSafeEquals(this.level, other.level) && nullSafeEquals(this.since, other.since);
	}

	/**
	 * Returns the hash code value for this ItemDeprecation object.
	 *
	 * The hash code is calculated based on the values of the reason, replacement, level,
	 * and since properties.
	 * @return the hash code value for this object
	 */
	@Override
	public int hashCode() {
		int result = nullSafeHashCode(this.reason);
		result = 31 * result + nullSafeHashCode(this.replacement);
		result = 31 * result + nullSafeHashCode(this.level);
		result = 31 * result + nullSafeHashCode(this.since);
		return result;
	}

	/**
	 * Returns a string representation of the ItemDeprecation object.
	 * @return a string representation of the ItemDeprecation object
	 */
	@Override
	public String toString() {
		return "ItemDeprecation{reason='" + this.reason + '\'' + ", replacement='" + this.replacement + '\''
				+ ", level='" + this.level + '\'' + ", since='" + this.since + '\'' + '}';
	}

	/**
	 * Compares two objects for equality, taking into account null values.
	 * @param o1 the first object to compare
	 * @param o2 the second object to compare
	 * @return true if the objects are equal, false otherwise
	 */
	private boolean nullSafeEquals(Object o1, Object o2) {
		if (o1 == o2) {
			return true;
		}
		if (o1 == null || o2 == null) {
			return false;
		}
		return o1.equals(o2);
	}

	/**
	 * Calculates the hash code of the given object in a null-safe manner.
	 * @param o the object whose hash code needs to be calculated
	 * @return the hash code of the object if it is not null, 0 otherwise
	 */
	private int nullSafeHashCode(Object o) {
		return (o != null) ? o.hashCode() : 0;
	}

}
