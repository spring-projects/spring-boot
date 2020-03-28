/*
 * Copyright 2012-2020 the original author or authors.
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

package org.springframework.boot.actuate.health;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Base {@link HealthAggregator} implementation to allow subclasses to focus on
 * aggregating the {@link Status} instances and not deal with contextual details etc.
 *
 * @author Christian Dupuis
 * @author Vedran Pavic
 * @since 1.1.0
 * @deprecated since 2.2.0 as {@link HealthAggregator} has been deprecated
 */
@Deprecated
public abstract class AbstractHealthAggregator implements HealthAggregator {

	@Override
	public final Health aggregate(Map<String, Health> healths) {
		List<Status> statusCandidates = healths.values().stream().map(Health::getStatus).collect(Collectors.toList());
		Status status = aggregateStatus(statusCandidates);
		Map<String, Object> details = aggregateDetails(healths);
		return new Health.Builder(status, details).build();
	}

	/**
	 * Return the single 'aggregate' status that should be used from the specified
	 * candidates.
	 * @param candidates the candidates
	 * @return a single status
	 */
	protected abstract Status aggregateStatus(List<Status> candidates);

	/**
	 * Return the map of 'aggregate' details that should be used from the specified
	 * healths.
	 * @param healths the health instances to aggregate
	 * @return a map of details
	 * @since 1.3.1
	 */
	protected Map<String, Object> aggregateDetails(Map<String, Health> healths) {
		return new LinkedHashMap<>(healths);
	}

}
