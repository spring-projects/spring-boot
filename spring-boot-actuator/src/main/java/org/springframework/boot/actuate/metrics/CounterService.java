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

/**
 * A service that can be used to increment, decrement and reset a {@link Metric}.
 * 
 * @author Dave Syer
 */
public interface CounterService {

	/**
	 * Increment the specified metric by 1.
	 * @param metricName the name of the metric
	 */
	void increment(String metricName);

	/**
	 * Decrement the specified metric by 1.
	 * @param metricName the name of the metric
	 */
	void decrement(String metricName);

	/**
	 * Reset the specified metric to 0.
	 * @param metricName the name of the metric
	 */
	void reset(String metricName);

}
