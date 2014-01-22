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

package org.springframework.boot.actuate.metrics.writer;

import org.springframework.boot.actuate.metrics.Metric;

/**
 * Basic strategy for write operations on {@link Metric} data.
 * 
 * @author Dave Syer
 */
public interface MetricWriter {

	/**
	 * Increment the value of a metric (or decrement if the delta is negative). The name
	 * of the delta is the name of the metric to increment.
	 * 
	 * @param delta the amount to increment by
	 */
	void increment(Delta<?> delta);

	/**
	 * Set the value of a metric.
	 * 
	 * @param value
	 */
	void set(Metric<?> value);

	/**
	 * Reset the value of a metric, usually to zero value. Implementations can discard the
	 * old values if desired, but may choose not to.
	 * 
	 * @param metricName the name to reset
	 */
	void reset(String metricName);

}
