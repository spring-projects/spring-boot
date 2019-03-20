/*
 * Copyright 2012-2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.actuate.metrics.repository;

import org.springframework.boot.actuate.metrics.reader.PrefixMetricReader;
import org.springframework.boot.actuate.metrics.writer.PrefixMetricWriter;

/**
 * A repository for metrics that allows efficient storage and retrieval of groups of
 * metrics with a common name prefix (their group name).
 *
 * @author Dave Syer
 */
public interface MultiMetricRepository extends PrefixMetricReader, PrefixMetricWriter {

	/**
	 * The names of all the groups known to this repository.
	 * @return all available group names
	 */
	Iterable<String> groups();

	/**
	 * The number of groups available in this repository.
	 * @return the number of groups
	 */
	long countGroups();

}
