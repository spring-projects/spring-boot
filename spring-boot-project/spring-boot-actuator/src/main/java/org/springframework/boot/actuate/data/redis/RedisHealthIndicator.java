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

import org.springframework.boot.actuate.health.AbstractHealthIndicator;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
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
 * @author Scott Frederick
 * @since 2.0.0
 */
public class RedisHealthIndicator extends AbstractHealthIndicator {

	private final RedisConnectionFactory redisConnectionFactory;

	/**
     * Constructs a new RedisHealthIndicator with the specified RedisConnectionFactory.
     * 
     * @param connectionFactory the RedisConnectionFactory to be used for health check
     * @throws IllegalArgumentException if the connectionFactory is null
     */
    public RedisHealthIndicator(RedisConnectionFactory connectionFactory) {
		super("Redis health check failed");
		Assert.notNull(connectionFactory, "ConnectionFactory must not be null");
		this.redisConnectionFactory = connectionFactory;
	}

	/**
     * Performs a health check on the Redis connection.
     * 
     * @param builder the Health.Builder object used to build the health status
     * @throws Exception if an error occurs during the health check
     */
    @Override
	protected void doHealthCheck(Health.Builder builder) throws Exception {
		RedisConnection connection = RedisConnectionUtils.getConnection(this.redisConnectionFactory);
		try {
			doHealthCheck(builder, connection);
		}
		finally {
			RedisConnectionUtils.releaseConnection(connection, this.redisConnectionFactory);
		}
	}

	/**
     * Performs a health check on the Redis connection.
     * 
     * @param builder the Health.Builder object to build the health status
     * @param connection the RedisConnection object to perform the health check on
     */
    private void doHealthCheck(Health.Builder builder, RedisConnection connection) {
		if (connection instanceof RedisClusterConnection clusterConnection) {
			RedisHealth.fromClusterInfo(builder, clusterConnection.clusterGetClusterInfo());
		}
		else {
			RedisHealth.up(builder, connection.serverCommands().info());
		}
	}

}
