/*
 * Copyright 2012-2022 the original author or authors.
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

package org.springframework.boot.actuate.data.redis;

import java.util.Properties;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Health.Builder;
import org.springframework.data.redis.connection.ClusterInfo;

/**
 * Shared class used by {@link RedisHealthIndicator} and
 * {@link RedisReactiveHealthIndicator} to provide health details.
 *
 * @author Phillip Webb
 */
final class RedisHealth {

	private RedisHealth() {
	}

	static Builder up(Health.Builder builder, Properties info) {
		builder.withDetail("version", info.getProperty("redis_version"));
		return builder.up();
	}

	static Builder fromClusterInfo(Health.Builder builder, ClusterInfo clusterInfo) {
		builder.withDetail("cluster_size", clusterInfo.getClusterSize());
		builder.withDetail("slots_up", clusterInfo.getSlotsOk());
		builder.withDetail("slots_fail", clusterInfo.getSlotsFail());

		if ("fail".equalsIgnoreCase(clusterInfo.getState())) {
			return builder.down();
		}
		else {
			return builder.up();
		}
	}

}
