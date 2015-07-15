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
		return reason;
	}

	public void setReason(String reason) {
		this.reason = reason;
	}

	public String getReplacement() {
		return replacement;
	}

	public void setReplacement(String replacement) {
		this.replacement = replacement;
	}

	@Override
	public String toString() {
		return "ItemDeprecation{" + "reason='" + this.reason + '\'' + ", " +
				"replacement='" + this.replacement + '\'' + '}';
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		ItemDeprecation that = (ItemDeprecation) o;

		if (reason != null ? !reason.equals(that.reason) : that.reason != null) return false;
		return !(replacement != null ? !replacement.equals(that.replacement) : that.replacement != null);

	}

	@Override
	public int hashCode() {
		int result = reason != null ? reason.hashCode() : 0;
		result = 31 * result + (replacement != null ? replacement.hashCode() : 0);
		return result;
	}
}
