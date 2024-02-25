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

	/**
	 * Private constructor for the RedisHealth class.
	 */
	private RedisHealth() {
	}

	/**
	 * Returns a new instance of the Health.Builder class with the "version" detail set to
	 * the value of the "redis_version" property from the provided Properties object.
	 * @param builder the Health.Builder object to modify
	 * @param info the Properties object containing the Redis version information
	 * @return a new instance of the Health.Builder class with the "version" detail set
	 * and marked as "up"
	 */
	static Builder up(Health.Builder builder, Properties info) {
		builder.withDetail("version", info.getProperty("redis_version"));
		return builder.up();
	}

	/**
	 * Creates a new instance of the {@link Builder} class using the provided
	 * {@link Health.Builder} and {@link ClusterInfo} objects.
	 * @param builder The {@link Health.Builder} object to use for building the health
	 * status.
	 * @param clusterInfo The {@link ClusterInfo} object containing the cluster
	 * information.
	 * @return A new instance of the {@link Builder} class with the health status set
	 * based on the cluster information.
	 */
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
