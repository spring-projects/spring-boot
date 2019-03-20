/*
 * Copyright 2012-2016 the original author or authors.
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

import java.util.Properties;

import org.springframework.data.redis.connection.ClusterInfo;
import org.springframework.data.redis.connection.RedisClusterConnection;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisConnectionUtils;
import org.springframework.util.Assert;

/**
 * Simple implementation of a {@link HealthIndicator} returning status information for
 * Redis data stores.
 *
 * @author Christian Dupuis
 * @author Richard Santana
 * @since 1.1.0
 */
public class RedisHealthIndicator extends AbstractHealthIndicator {

	private static final String VERSION = "version";

	private static final String REDIS_VERSION = "redis_version";

	private final RedisConnectionFactory redisConnectionFactory;

	public RedisHealthIndicator(RedisConnectionFactory connectionFactory) {
		Assert.notNull(connectionFactory, "ConnectionFactory must not be null");
		this.redisConnectionFactory = connectionFactory;
	}

	@Override
	protected void doHealthCheck(Health.Builder builder) throws Exception {
		RedisConnection connection = RedisConnectionUtils
				.getConnection(this.redisConnectionFactory);
		try {
			if (connection instanceof RedisClusterConnection) {
				ClusterInfo clusterInfo = ((RedisClusterConnection) connection)
						.clusterGetClusterInfo();
				builder.up().withDetail("cluster_size", clusterInfo.getClusterSize())
						.withDetail("slots_up", clusterInfo.getSlotsOk())
						.withDetail("slots_fail", clusterInfo.getSlotsFail());
			}
			else {
				Properties info = connection.info();
				builder.up().withDetail(VERSION, info.getProperty(REDIS_VERSION));
			}
		}
		finally {
			RedisConnectionUtils.releaseConnection(connection,
					this.redisConnectionFactory);
		}
	}

}
