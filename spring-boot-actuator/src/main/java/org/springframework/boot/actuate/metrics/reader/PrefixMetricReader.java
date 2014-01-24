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

package org.springframework.boot.actuate.metrics.reader;

import org.springframework.boot.actuate.metrics.Metric;

/**
 * Interface for extracting metrics as a group whose name starts with a prefix.
 * 
 * @author Dave Syer
 */
public interface PrefixMetricReader {

	/**
	 * Find all metrics whose name starts with the given prefix.
	 * @param prefix the prefix for metric names
	 * @return all metrics with names starting with the prefix
	 */
	Iterable<Metric<?>> findAll(String prefix);

}
