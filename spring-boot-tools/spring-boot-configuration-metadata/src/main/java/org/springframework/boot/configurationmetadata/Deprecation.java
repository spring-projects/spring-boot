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

package org.springframework.boot.configurationmetadata;

import java.io.Serializable;

/**
 * Indicate that a property is deprecated. Provide additional information about the
 * deprecation.
 *
 * @author Stephane Nicoll
 * @since 1.3.0
 */
@SuppressWarnings("serial")
public class Deprecation implements Serializable {

	private String reason;

	private String replacement;

	/**
	 * A reason why the related property is deprecated, if any. Can be multi-lines.
	 * @return the deprecation reason
	 */
	public String getReason() {
		return this.reason;
	}

	public void setReason(String reason) {
		this.reason = reason;
	}

	/**
	 * The full name of the property that replaces the related deprecated property, if
	 * any.
	 * @return the replacement property name
	 */
	public String getReplacement() {
		return this.replacement;
	}

	public void setReplacement(String replacement) {
		this.replacement = replacement;
	}

	@Override
	public String toString() {
		return "Deprecation{" + "reason='" + this.reason + '\'' + ", replacement='"
				+ this.replacement + '\'' + '}';
	}

}
