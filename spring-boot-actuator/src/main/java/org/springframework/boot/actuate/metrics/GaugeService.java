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
 * A service that can be used to submit a named double value for storage and analysis. Any
 * statistics or analysis that needs to be carried out is best left for other concerns,
 * but ultimately they are under control of the implementation of this service. For
 * instance, the value submitted here could be a method execution timing result, and it
 * would go to a backend that keeps a histogram of recent values for comparison purposes.
 * Or it could be a simple measurement of a sensor value (like a temperature reading) to
 * be passed on to a monitoring system in its raw form.
 * 
 * @author Dave Syer
 */
public interface GaugeService {

	/**
	 * Set the specified gauge value
	 * @param metricName the name of the gauge to set
	 * @param value the value of the gauge
	 */
	void submit(String metricName, double value);

}
