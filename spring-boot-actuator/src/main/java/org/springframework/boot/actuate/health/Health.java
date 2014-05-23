/*
 * Copyright 2012-2014 the original author or authors.
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

package org.springframework.boot.actuate.health;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.util.Assert;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonUnwrapped;

/**
 * Carries information about the health of a component or subsystem.
 * <p>
 * {@link Health} contains a {@link Status} to express the state of a component or
 * subsystem and some additional details to carry some contextual information.
 * <p>
 * {@link Health} has a fluent API to make it easy to construct instances. Typical usage
 * in a {@link HealthIndicator} would be:
 * 
 * <pre class="code">
 * try {
 * 	// do some test to determine state of component
 * 	return Health.up(&quot;version&quot;, &quot;1.1.2&quot;);
 * }
 * catch (Exception ex) {
 * 	return Health.down(ex);
 * }
 * </pre>
 * 
 * @author Christian Dupuis
 * @author Phillip Webb
 * @since 1.1.0
 */
@JsonInclude(Include.NON_EMPTY)
public final class Health {

	private static final Map<String, Object> NO_DETAILS = Collections
			.<String, Object> emptyMap();

	private final Status status;

	private final Map<String, Object> details;

	/**
	 * Create a new {@link Health} instance with the specified status and details.
	 * @param status the status
	 * @param details the details or {@code null}
	 */
	public Health(Status status, Map<String, ?> details) {
		Assert.notNull(status, "Status must not be null");
		this.status = status;
		this.details = Collections.unmodifiableMap(details == null ? NO_DETAILS
				: new LinkedHashMap<String, Object>(details));
	}

	/**
	 * @return the status of the health (never {@code null})
	 */
	@JsonUnwrapped
	public Status getStatus() {
		return this.status;
	}

	/**
	 * @return the details of the health or an empty map.
	 */
	@JsonAnyGetter
	public Map<String, Object> getDetails() {
		return this.details;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this) {
			return true;
		}
		if (obj != null && obj instanceof Health) {
			Health other = (Health) obj;
			return this.status.equals(other.status) && this.details.equals(other.details);
		}
		return false;
	}

	@Override
	public int hashCode() {
		int hashCode = this.status.hashCode();
		return 13 * hashCode + this.details.hashCode();
	}

	@Override
	public String toString() {
		return getStatus() + " " + getDetails();
	}

	/**
	 * Create a new {@link Health} object from this one, containing an additional
	 * exception detail.
	 * @param ex the exception
	 * @return a new {@link Health} instance
	 */
	public Health withException(Exception ex) {
		Assert.notNull(ex, "Exception must not be null");
		return withDetail("error", ex.getClass().getName() + ": " + ex.getMessage());
	}

	/**
	 * Create a new {@link Health} object from this one, containing an additional detail.
	 * @param key the detail key
	 * @param data the detail data
	 * @return a new {@link Health} instance
	 */
	@JsonAnySetter
	public Health withDetail(String key, Object data) {
		Assert.notNull(key, "Key must not be null");
		Assert.notNull(data, "Data must not be null");
		Map<String, Object> details = new LinkedHashMap<String, Object>(this.details);
		details.put(key, data);
		return new Health(this.status, details);
	}

	/**
	 * Create a new {@link Health} instance with an {@link Status#UNKNOWN} status.
	 * @return a new {@link Health} instance
	 */
	public static Health unknown() {
		return status(Status.UNKNOWN);
	}

	/**
	 * Create a new {@link Health} instance with an {@link Status#UP} status.
	 * @return a new {@link Health} instance
	 */
	public static Health up() {
		return status(Status.UP);
	}

	/**
	 * Create a new {@link Health} instance with an {@link Status#DOWN} status an the
	 * specified exception details.
	 * @param ex the exception
	 * @return a new {@link Health} instance
	 */
	public static Health down(Exception ex) {
		return down().withException(ex);
	}

	/**
	 * Create a new {@link Health} instance with a {@link Status#DOWN} status.
	 * @return a new {@link Health} instance
	 */
	public static Health down() {
		return status(Status.DOWN);
	}

	/**
	 * Create a new {@link Health} instance with an {@link Status#OUT_OF_SERVICE} status.
	 * @return a new {@link Health} instance
	 */
	public static Health outOfService() {
		return status(Status.OUT_OF_SERVICE);
	}

	/**
	 * Create a new {@link Health} instance with a specific status code.
	 * @return a new {@link Health} instance
	 */
	public static Health status(String statusCode) {
		return status(new Status(statusCode));
	}

	/**
	 * Create a new {@link Health} instance with a specific {@link Status}.
	 * @return a new {@link Health} instance
	 */
	public static Health status(Status status) {
		return new Health(status, null);
	}

}
