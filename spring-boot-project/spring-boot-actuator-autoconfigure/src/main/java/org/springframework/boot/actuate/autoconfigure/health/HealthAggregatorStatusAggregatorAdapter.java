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

package org.springframework.boot.actuate.autoconfigure.health;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;
import org.springframework.boot.actuate.health.StatusAggregator;

/**
 * Adapter class to convert a legacy
 * {@link org.springframework.boot.actuate.health.HealthAggregator} to a
 * {@link StatusAggregator}.
 *
 * @author Phillip Webb
 */
@SuppressWarnings("deprecation")
class HealthAggregatorStatusAggregatorAdapter implements StatusAggregator {

	private org.springframework.boot.actuate.health.HealthAggregator healthAggregator;

	HealthAggregatorStatusAggregatorAdapter(org.springframework.boot.actuate.health.HealthAggregator healthAggregator) {
		this.healthAggregator = healthAggregator;
	}

	@Override
	public Status getAggregateStatus(Set<Status> statuses) {
		int index = 0;
		Map<String, Health> healths = new LinkedHashMap<>();
		for (Status status : statuses) {
			index++;
			healths.put("health" + index, asHealth(status));
		}
		Health aggregate = this.healthAggregator.aggregate(healths);
		return aggregate.getStatus();
	}

	private Health asHealth(Status status) {
		return Health.status(status).build();
	}

}
