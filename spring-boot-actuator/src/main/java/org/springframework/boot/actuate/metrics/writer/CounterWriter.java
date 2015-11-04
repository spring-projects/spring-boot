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

package org.springframework.boot.actuate.metrics.writer;

/**
 * Simple writer for counters (metrics that increment).
 *
 * @author Dave Syer
 * @since 1.3.0
 */
public interface CounterWriter {

	/**
	 * Increment the value of a metric (or decrement if the delta is negative). The name
	 * of the delta is the name of the metric to increment.
	 * @param delta the amount to increment by
	 */
	void increment(Delta<?> delta);

	/**
	 * Reset the value of a metric, usually to zero value. Implementations can discard the
	 * old values if desired, but may choose not to. This operation is optional (some
	 * implementations may not be able to fulfill the contract, in which case they should
	 * simply do nothing).
	 * @param metricName the name to reset
	 */
	void reset(String metricName);

}
