/*
 * Copyright 2012-2016 the original author or authors.
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

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonUnwrapped;

import org.springframework.util.Assert;

/**
 * Carries information about the health of a component or subsystem.
 * <p>
 * {@link Health} contains a {@link Status} to express the state of a component or
 * subsystem and some additional details to carry some contextual information.
 * <p>
 * {@link Health} instances can be created by using {@link Builder}'s fluent API. Typical
 * usage in a {@link HealthIndicator} would be:
 *
 * <pre class="code">
 * try {
 * 	// do some test to determine state of component
 * 	return new Health.Builder().up().withDetail("version", "1.1.2").build();
 * }
 * catch (Exception ex) {
 * 	return new Health.Builder().down(ex).build();
 * }
 * </pre>
 *
 * @author Christian Dupuis
 * @author Phillip Webb
 * @since 1.1.0
 */
@JsonInclude(Include.NON_EMPTY)
public final class Health {

	private final Status status;

	private final Map<String, Object> details;

	/**
	 * Create a new {@link Health} instance with the specified status and details.
	 * @param builder the Builder to use
	 */
	private Health(Builder builder) {
		Assert.notNull(builder, "Builder must not be null");
		this.status = builder.status;
		this.details = Collections.unmodifiableMap(builder.details);
	}

	/**
	 * Return the status of the health.
	 * @return the status (never {@code null})
	 */
	@JsonUnwrapped
	public Status getStatus() {
		return this.status;
	}

	/**
	 * Return the details of the health.
	 * @return the details (or an empty map)
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
	 * Create a new {@link Builder} instance with an {@link Status#UNKNOWN} status.
	 * @return a new {@link Builder} instance
	 */
	public static Builder unknown() {
		return status(Status.UNKNOWN);
	}

	/**
	 * Create a new {@link Builder} instance with an {@link Status#UP} status.
	 * @return a new {@link Builder} instance
	 */
	public static Builder up() {
		return status(Status.UP);
	}

	/**
	 * Create a new {@link Builder} instance with an {@link Status#DOWN} status an the
	 * specified exception details.
	 * @param ex the exception
	 * @return a new {@link Builder} instance
	 */
	public static Builder down(Exception ex) {
		return down().withException(ex);
	}

	/**
	 * Create a new {@link Builder} instance with a {@link Status#DOWN} status.
	 * @return a new {@link Builder} instance
	 */
	public static Builder down() {
		return status(Status.DOWN);
	}

	/**
	 * Create a new {@link Builder} instance with an {@link Status#OUT_OF_SERVICE} status.
	 * @return a new {@link Builder} instance
	 */
	public static Builder outOfService() {
		return status(Status.OUT_OF_SERVICE);
	}

	/**
	 * Create a new {@link Builder} instance with a specific status code.
	 * @param statusCode the status code
	 * @return a new {@link Builder} instance
	 */
	public static Builder status(String statusCode) {
		return status(new Status(statusCode));
	}

	/**
	 * Create a new {@link Builder} instance with a specific {@link Status}.
	 * @param status the status
	 * @return a new {@link Builder} instance
	 */
	public static Builder status(Status status) {
		return new Builder(status);
	}

	/**
	 * Builder for creating immutable {@link Health} instances.
	 */
	public static class Builder {

		private Status status;

		private Map<String, Object> details;

		/**
		 * Create new Builder instance.
		 */
		public Builder() {
			this.status = Status.UNKNOWN;
			this.details = new LinkedHashMap<String, Object>();
		}

		/**
		 * Create new Builder instance, setting status to given {@code status}.
		 * @param status the {@link Status} to use
		 */
		public Builder(Status status) {
			Assert.notNull(status, "Status must not be null");
			this.status = status;
			this.details = new LinkedHashMap<String, Object>();
		}

		/**
		 * Create new Builder instance, setting status to given {@code status} and details
		 * to given {@code details}.
		 * @param status the {@link Status} to use
		 * @param details the details {@link Map} to use
		 */
		public Builder(Status status, Map<String, ?> details) {
			Assert.notNull(status, "Status must not be null");
			Assert.notNull(details, "Details must not be null");
			this.status = status;
			this.details = new LinkedHashMap<String, Object>(details);
		}

		/**
		 * Record detail for given {@link Exception}.
		 * @param ex the exception
		 * @return this {@link Builder} instance
		 */
		public Builder withException(Exception ex) {
			Assert.notNull(ex, "Exception must not be null");
			return withDetail("error", ex.getClass().getName() + ": " + ex.getMessage());
		}

		/**
		 * Record detail using given {@code key} and {@code value}.
		 * @param key the detail key
		 * @param value the detail value
		 * @return this {@link Builder} instance
		 */
		public Builder withDetail(String key, Object value) {
			Assert.notNull(key, "Key must not be null");
			Assert.notNull(value, "Value must not be null");
			this.details.put(key, value);
			return this;
		}

		/**
		 * Set status to {@link Status#UNKNOWN} status.
		 * @return this {@link Builder} instance
		 */
		public Builder unknown() {
			return status(Status.UNKNOWN);
		}

		/**
		 * Set status to {@link Status#UP} status.
		 * @return this {@link Builder} instance
		 */
		public Builder up() {
			return status(Status.UP);
		}

		/**
		 * Set status to {@link Status#DOWN} and add details for given {@link Exception}.
		 * @param ex the exception
		 * @return this {@link Builder} instance
		 */
		public Builder down(Exception ex) {
			return down().withException(ex);
		}

		/**
		 * Set status to {@link Status#DOWN}.
		 * @return this {@link Builder} instance
		 */
		public Builder down() {
			return status(Status.DOWN);
		}

		/**
		 * Set status to {@link Status#OUT_OF_SERVICE}.
		 * @return this {@link Builder} instance
		 */
		public Builder outOfService() {
			return status(Status.OUT_OF_SERVICE);
		}

		/**
		 * Set status to given {@code statusCode}.
		 * @param statusCode the status code
		 * @return this {@link Builder} instance
		 */
		public Builder status(String statusCode) {
			return status(new Status(statusCode));
		}

		/**
		 * Set status to given {@link Status} instance.
		 * @param status the status
		 * @return this {@link Builder} instance
		 */
		public Builder status(Status status) {
			this.status = status;
			return this;
		}

		/**
		 * Create a new {@link Health} instance with the previously specified code and
		 * details.
		 * @return a new {@link Health} instance
		 */
		public Health build() {
			return new Health(this);
		}

	}

}
