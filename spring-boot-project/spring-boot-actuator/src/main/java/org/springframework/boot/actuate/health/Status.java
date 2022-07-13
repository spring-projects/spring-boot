/*
 * Copyright 2012-2022 the original author or authors.
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

package org.springframework.boot.actuate.health;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;

/**
 * Value object to express state of a component or subsystem.
 * <p>
 * Status provides convenient constants for commonly used states like {@link #UP},
 * {@link #DOWN} or {@link #OUT_OF_SERVICE}.
 * <p>
 * Custom states can also be created and used throughout the Spring Boot Health subsystem.
 *
 * @author Christian Dupuis
 * @since 1.1.0
 */
@JsonInclude(Include.NON_EMPTY)
public final class Status {

	/**
	 * {@link Status} indicating that the component or subsystem is in an unknown state.
	 */
	public static final Status UNKNOWN = new Status("UNKNOWN");

	/**
	 * {@link Status} indicating that the component or subsystem is functioning as
	 * expected.
	 */
	public static final Status UP = new Status("UP");

	/**
	 * {@link Status} indicating that the component or subsystem has suffered an
	 * unexpected failure.
	 */
	public static final Status DOWN = new Status("DOWN");

	/**
	 * {@link Status} indicating that the component or subsystem has been taken out of
	 * service and should not be used.
	 */
	public static final Status OUT_OF_SERVICE = new Status("OUT_OF_SERVICE");

	private final String code;

	private final String description;

	/**
	 * Create a new {@link Status} instance with the given code and an empty description.
	 * @param code the status code
	 */
	public Status(String code) {
		this(code, "");
	}

	/**
	 * Create a new {@link Status} instance with the given code and description.
	 * @param code the status code
	 * @param description a description of the status
	 */
	public Status(String code, String description) {
		Assert.notNull(code, "Code must not be null");
		Assert.notNull(description, "Description must not be null");
		this.code = code;
		this.description = description;
	}

	/**
	 * Return the code for this status.
	 * @return the code
	 */
	@JsonProperty("status")
	public String getCode() {
		return this.code;
	}

	/**
	 * Return the description of this status.
	 * @return the description
	 */
	@JsonInclude(Include.NON_EMPTY)
	public String getDescription() {
		return this.description;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this) {
			return true;
		}
		if (obj instanceof Status other) {
			return ObjectUtils.nullSafeEquals(this.code, other.code);
		}
		return false;
	}

	@Override
	public int hashCode() {
		return this.code.hashCode();
	}

	@Override
	public String toString() {
		return this.code;
	}

}
