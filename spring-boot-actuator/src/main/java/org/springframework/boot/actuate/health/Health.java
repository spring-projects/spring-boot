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

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonUnwrapped;

/**
 * Value object used to carry information about the health information of a component or
 * subsystem.
 * 
 * <p>
 * {@link Health} contains a {@link Status} to express the state of a component or
 * subsystem and some additional details to carry some contextual information.
 * 
 * <p>
 * {@link Health} has a fluent API to make it easy to construct instances. Typical usage
 * in a {@link HealthIndicator} would be:
 * 
 * <code>
 * 		Health health = new Health();
 * 		try {
 * 			// do some test to determine state of component
 * 
 * 			health.up().withDetail("version", "1.1.2");
 * 		}
 * 		catch (Exception ex) {
 * 			health.down().withException(ex);
 * 		}
 * 		return health;
 * </code>
 * 
 * @author Christian Dupuis
 * @since 1.1.0
 */
@JsonInclude(Include.NON_EMPTY)
public class Health {

	private Status status;

	private Map<String, Object> details;

	public Health() {
		this(Status.UNKOWN);
	}

	public Health(Status status) {
		this.status = status;
		this.details = new LinkedHashMap<String, Object>();
	}

	public Health status(Status status) {
		Assert.notNull(status, "Status must not be null");
		this.status = status;
		return this;
	}

	public Health up() {
		return status(Status.UP);
	}

	public Health down() {
		return status(Status.DOWN);
	}

	public Health withException(Exception ex) {
		Assert.notNull(ex, "Exception must not be null");
		return withDetail("error", ex.getClass().getName() + ": " + ex.getMessage());
	}

	@JsonAnySetter
	public Health withDetail(String key, Object data) {
		Assert.notNull(key, "Key must not be null");
		Assert.notNull(data, "Data must not be null");
		this.details.put(key, data);
		return this;
	}

	@JsonUnwrapped
	public Status getStatus() {
		return this.status;
	}

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
			return ObjectUtils.nullSafeEquals(this.status, ((Health) obj).status)
					&& ObjectUtils.nullSafeEquals(this.details, ((Health) obj).details);
		}
		return false;
	}

	@Override
	public int hashCode() {
		int hashCode = 0;
		if (this.status != null) {
			hashCode = this.status.hashCode();
		}
		return 13 * hashCode + this.details.hashCode();
	}

}
