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

package org.springframework.bootstrap.actuate.metrics;

/**
 * A service that can be used to manage a {@link Metric} as a gauge.
 * 
 * @author Dave Syer
 */
public interface GaugeService {

	/**
	 * Set the specified metric value
	 * @param metricName the metric to set
	 * @param value the value of the metric
	 */
	void set(String metricName, double value);

}
