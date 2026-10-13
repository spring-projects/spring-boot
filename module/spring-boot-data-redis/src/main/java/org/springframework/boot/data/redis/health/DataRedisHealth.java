/*
 * Copyright 2012-present the original author or authors.
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
		builder.withDetail("version", info.getProperty("redis_version", "unknown"));
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
