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

import java.util.Date;

import org.springframework.util.ObjectUtils;

/**
 * A {@link Metric} at a given point in time.
 * 
 * @author Dave Syer
 */
public final class Measurement {

	private Date timestamp;

	private Metric metric;

	public Measurement(Date timestamp, Metric metric) {
		this.timestamp = timestamp;
		this.metric = metric;
	}

	public Date getTimestamp() {
		return this.timestamp;
	}

	public Metric getMetric() {
		return this.metric;
	}

	@Override
	public String toString() {
		return "Measurement [dateTime=" + this.timestamp + ", metric=" + this.metric
				+ "]";
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ObjectUtils.nullSafeHashCode(this.timestamp);
		result = prime * result + ObjectUtils.nullSafeHashCode(this.metric);
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
			Measurement other = (Measurement) obj;
			boolean result = ObjectUtils.nullSafeEquals(this.timestamp, other.timestamp);
			result &= ObjectUtils.nullSafeEquals(this.metric, other.metric);
			return result;
		}
		return super.equals(obj);
	}

}
