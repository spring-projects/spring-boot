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

package org.springframework.boot.actuate.metrics.repository;

import java.util.Collection;

import org.springframework.boot.actuate.metrics.Metric;
import org.springframework.boot.actuate.metrics.reader.PrefixMetricReader;

/**
 * A repository for metrics that allows efficient storage and retrieval of groups of
 * metrics with a common name prefix (their group name).
 * 
 * @author Dave Syer
 */
public interface MultiMetricRepository extends PrefixMetricReader {

	/**
	 * Save some metric values and associate them with a group name.
	 * 
	 * @param group the name of the group
	 * @param values the metric values to save
	 */
	void save(String group, Collection<Metric<?>> values);

	/**
	 * Rest the values of all metrics in the group. Implementations may choose to discard
	 * the old values.
	 * 
	 * @param group reset the whole group
	 */
	void reset(String group);

	/**
	 * The names of all the groups known to this repository
	 * 
	 * @return all available group names
	 */
	Iterable<String> groups();

	/**
	 * @return the number of groups available
	 */
	long count();

}
