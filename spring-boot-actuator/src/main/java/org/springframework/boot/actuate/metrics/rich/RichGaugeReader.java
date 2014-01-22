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

package org.springframework.boot.actuate.metrics.rich;

/**
 * A basic set of read operations for {@link RichGauge} instances.
 * 
 * @author Dave Syer
 */
public interface RichGaugeReader {

	/**
	 * Find a single instance of a rich gauge by name.
	 * 
	 * @param name the name of the gauge
	 * @return a rich gauge value
	 */
	RichGauge findOne(String name);

	/**
	 * Find all instances of rich gauge known to this reader.
	 * 
	 * @return all instances known to this reader
	 */
	Iterable<RichGauge> findAll();

	/**
	 * @return the number of gauge values available
	 */
	long count();

}
