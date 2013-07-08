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

package org.springframework.zero.actuate.metrics;

import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;

/**
 * Immutable class that can be used to hold any arbitrary system measurement value. For
 * example a metric might record the number of active connections.
 * 
 * @author Dave Syer
 * @see MetricRepository
 * @see CounterService
 */
public final class Metric {

	private final String name;

	private final double value;

	/**
	 * Create a new {@link Metric} instance.
	 * @param name the name of the metric
	 * @param value the value of the metric
	 */
	public Metric(String name, double value) {
		super();
		Assert.notNull(name, "Name must not be null");
		this.name = name;
		this.value = value;
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
	public double getValue() {
		return this.value;
	}

	/**
	 * Create a new {@link Metric} with an incremented value.
	 * @param amount the amount that the new metric will differ from this one
	 * @return a new {@link Metric} instance
	 */
	public Metric increment(int amount) {
		return new Metric(this.name, new Double(((int) this.value) + amount));
	}

	/**
	 * Create a new {@link Metric} with a different value.
	 * @param value the value of the new metric
	 * @return a new {@link Metric} instance
	 */
	public Metric set(double value) {
		return new Metric(this.name, value);
	}

	@Override
	public String toString() {
		return "Metric [name=" + this.name + ", value=" + this.value + "]";
	}

	@Override
	public int hashCode() {
		int valueHashCode = ObjectUtils.hashCode(this.value);
		final int prime = 31;
		int result = 1;
		result = prime * result + ObjectUtils.nullSafeHashCode(this.name);
		result = prime * result + (valueHashCode ^ (valueHashCode >>> 32));
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() == obj.getClass()) {
			Metric other = (Metric) obj;
			boolean result = ObjectUtils.nullSafeEquals(this.name, other.name);
			result &= Double.doubleToLongBits(this.value) == Double
					.doubleToLongBits(other.value);
			return result;
		}
		return super.equals(obj);
	}

}
