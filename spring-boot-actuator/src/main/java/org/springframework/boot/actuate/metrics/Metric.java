/*
 * Copyright 2012-2013 the original author or authors.
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

package org.springframework.boot.actuate.metrics;

import java.util.Date;

import org.springframework.util.Assert;

/**
 * Immutable class that can be used to hold any arbitrary system measurement value (a
 * named numeric value with a timestamp). For example a metric might record the number of
 * active connections to a server, or the temperature of a meeting room.
 * 
 * @author Dave Syer
 */
public class Metric<T extends Number> {

	private final String name;

	private final T value;

	private Date timestamp;

	/**
	 * Create a new {@link Metric} instance for the current time.
	 * @param name the name of the metric
	 * @param value the value of the metric
	 */
	public Metric(String name, T value) {
		this(name, value, new Date());
	}

	/**
	 * Create a new {@link Metric} instance.
	 * @param name the name of the metric
	 * @param value the value of the metric
	 * @param timestamp the timestamp for the metric
	 */
	public Metric(String name, T value, Date timestamp) {
		Assert.notNull(name, "Name must not be null");
		this.name = name;
		this.value = value;
		this.timestamp = timestamp;
	}

	/**
	 * Returns the name of the metric.
	 */
	public String getName() {
		return this.name;
	}

	/**
	 * Returns the value of the metric.
	 */
	public T getValue() {
		return this.value;
	}

	public Date getTimestamp() {
		return this.timestamp;
	}

	@Override
	public String toString() {
		return "Metric [name=" + this.name + ", value=" + this.value + ", timestamp="
				+ this.timestamp + "]";
	}

	/**
	 * Create a new {@link Metric} with an incremented value.
	 * @param amount the amount that the new metric will differ from this one
	 * @return a new {@link Metric} instance
	 */
	public Metric<Long> increment(int amount) {
		return new Metric<Long>(this.getName(), new Long(this.getValue().longValue()
				+ amount));
	}

	/**
	 * Create a new {@link Metric} with a different value.
	 * @param value the value of the new metric
	 * @return a new {@link Metric} instance
	 */
	public <S extends Number> Metric<S> set(S value) {
		return new Metric<S>(this.getName(), value);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((this.name == null) ? 0 : this.name.hashCode());
		result = prime * result
				+ ((this.timestamp == null) ? 0 : this.timestamp.hashCode());
		result = prime * result + ((this.value == null) ? 0 : this.value.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Metric<?> other = (Metric<?>) obj;
		if (this.name == null) {
			if (other.name != null)
				return false;
		}
		else if (!this.name.equals(other.name))
			return false;
		if (this.timestamp == null) {
			if (other.timestamp != null)
				return false;
		}
		else if (!this.timestamp.equals(other.timestamp))
			return false;
		if (this.value == null) {
			if (other.value != null)
				return false;
		}
		else if (!this.value.equals(other.value))
			return false;
		return true;
	}

}
