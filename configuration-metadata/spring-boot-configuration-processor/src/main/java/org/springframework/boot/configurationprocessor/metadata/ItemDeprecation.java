/*
 * Copyright 2012-present the original author or authors.
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

	public ItemDeprecation() {
		this(null, null, null);
	}

	public ItemDeprecation(String reason, String replacement, String since) {
		this(reason, replacement, since, null);
	}

	public ItemDeprecation(String reason, String replacement, String since, String level) {
		this.reason = reason;
		this.replacement = replacement;
		this.since = since;
		this.level = level;
	}

	public String getReason() {
		return this.reason;
	}

	public void setReason(String reason) {
		this.reason = reason;
	}

	public String getReplacement() {
		return this.replacement;
	}

	public void setReplacement(String replacement) {
		this.replacement = replacement;
	}

	public String getSince() {
		return this.since;
	}

	public void setSince(String since) {
		this.since = since;
	}

	public String getLevel() {
		return this.level;
	}

	public void setLevel(String level) {
		this.level = level;
	}

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

	@Override
	public int hashCode() {
		int result = nullSafeHashCode(this.reason);
		result = 31 * result + nullSafeHashCode(this.replacement);
		result = 31 * result + nullSafeHashCode(this.level);
		result = 31 * result + nullSafeHashCode(this.since);
		return result;
	}

	@Override
	public String toString() {
		return "ItemDeprecation{reason='" + this.reason + '\'' + ", replacement='" + this.replacement + '\''
				+ ", level='" + this.level + '\'' + ", since='" + this.since + '\'' + '}';
	}

	private boolean nullSafeEquals(Object o1, Object o2) {
		if (o1 == o2) {
			return true;
		}
		if (o1 == null || o2 == null) {
			return false;
		}
		return o1.equals(o2);
	}

	private int nullSafeHashCode(Object o) {
		return (o != null) ? o.hashCode() : 0;
	}

}
