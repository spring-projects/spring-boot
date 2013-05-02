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

package org.springframework.bootstrap.service.metrics;

import java.util.Date;

/**
 * @author Dave Syer
 */
public class Measurement {

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
		return "Measurement [dateTime=" + this.timestamp + ", metric=" + this.metric + "]";
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((this.timestamp == null) ? 0 : this.timestamp.hashCode());
		result = prime * result + ((this.metric == null) ? 0 : this.metric.hashCode());
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
		Measurement other = (Measurement) obj;
		if (this.timestamp == null) {
			if (other.timestamp != null)
				return false;
		} else if (!this.timestamp.equals(other.timestamp))
			return false;
		if (this.metric == null) {
			if (other.metric != null)
				return false;
		} else if (!this.metric.equals(other.metric))
			return false;
		return true;
	}

}
