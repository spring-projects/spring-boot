/*
 * Copyright 2012-2014 the original author or authors.
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

package org.springframework.boot.actuate.health;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Base {@link HealthAggregator} implementation to allow subclasses to focus on
 * aggregating the {@link Status} instances and not deal with contextual details etc.
 *
 * @author Christian Dupuis
 * @since 1.1.0
 */
public abstract class AbstractHealthAggregator implements HealthAggregator {

	@Override
	public final Health aggregate(Map<String, Health> healths) {
		List<Status> statusCandidates = new ArrayList<Status>();
		Map<String, Object> details = new LinkedHashMap<String, Object>();
		for (Map.Entry<String, Health> entry : healths.entrySet()) {
			details.put(entry.getKey(), entry.getValue());
			statusCandidates.add(entry.getValue().getStatus());
		}
		return new Health.Builder(aggregateStatus(statusCandidates), details).build();
	}

	/**
	 * Return the single 'aggregate' status that should be used from the specified
	 * candidates.
	 * @param candidates
	 * @return a single status
	 */
	protected abstract Status aggregateStatus(List<Status> candidates);

}
