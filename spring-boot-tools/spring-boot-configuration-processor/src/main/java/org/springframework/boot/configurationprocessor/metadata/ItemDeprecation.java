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

package org.springframework.boot.configurationprocessor.metadata;

/**
 * Describe an item deprecation.
 *
 * @author Stephane Nicoll
 * @since 1.3.0
 */
public class ItemDeprecation {

	private String reason;

	private String replacement;

	public ItemDeprecation() {
	}

	public ItemDeprecation(String reason, String replacement) {
		this.reason = reason;
		this.replacement = replacement;
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

	@Override
	public String toString() {
		return "ItemDeprecation{" + "reason='" + this.reason + '\'' + ", "
				+ "replacement='" + this.replacement + '\'' + '}';
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
		return nullSafeEquals(this.reason, other.reason)
				&& nullSafeEquals(this.replacement, other.replacement);
	}

	@Override
	public int hashCode() {
		int result = nullSafeHashCode(this.reason);
		result = 31 * result + nullSafeHashCode(this.replacement);
		return result;
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
		return (o == null ? 0 : o.hashCode());
	}

}
