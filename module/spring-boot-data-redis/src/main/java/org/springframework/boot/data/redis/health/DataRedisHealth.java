/*
 * Copyright 2012-present the original author or authors.
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

package org.springframework.boot.data.redis.health;

import java.util.Properties;

import org.springframework.boot.health.contributor.Health;
import org.springframework.data.redis.connection.ClusterInfo;

/**
 * Shared class used by {@link DataRedisHealthIndicator} and
 * {@link DataRedisReactiveHealthIndicator} to provide health details.
 *
 * @author Phillip Webb
 */
final class DataRedisHealth {

	private DataRedisHealth() {
	}

	static Health.Builder up(Health.Builder builder, Properties info) {
		builder.withDetail("version", info.getProperty("redis_version"));
		return builder.up();
	}

	static Health.Builder fromClusterInfo(Health.Builder builder, ClusterInfo clusterInfo) {
		Long clusterSize = clusterInfo.getClusterSize();
		if (clusterSize != null) {
			builder.withDetail("cluster_size", clusterSize);
		}
		Long slotsOk = clusterInfo.getSlotsOk();
		if (slotsOk != null) {
			builder.withDetail("slots_up", slotsOk);
		}
		Long slotsFail = clusterInfo.getSlotsFail();
		if (slotsFail != null) {
			builder.withDetail("slots_fail", slotsFail);
		}
		if ("fail".equalsIgnoreCase(clusterInfo.getState())) {
			return builder.down();
		}
		else {
			return builder.up();
		}
	}

}
