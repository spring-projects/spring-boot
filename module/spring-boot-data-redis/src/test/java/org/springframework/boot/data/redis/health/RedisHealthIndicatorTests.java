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

import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.Status;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.connection.ClusterInfo;
import org.springframework.data.redis.connection.RedisClusterConnection;
import org.springframework.data.redis.connection.RedisClusterNode;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisServerCommands;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link DataRedisHealthIndicator}.
 *
 * @author Christian Dupuis
 * @author Richard Santana
 * @author Stephane Nicoll
 */
class RedisHealthIndicatorTests {

	@Test
	void redisIsUp() {
		Properties info = new Properties();
		info.put("redis_version", "2.8.9");
		RedisConnection redisConnection = mock(RedisConnection.class);
		RedisServerCommands serverCommands = mock(RedisServerCommands.class);
		given(redisConnection.serverCommands()).willReturn(serverCommands);
		given(serverCommands.info()).willReturn(info);
		DataRedisHealthIndicator healthIndicator = createHealthIndicator(redisConnection);
		Health health = healthIndicator.health();
		assertThat(health.getStatus()).isEqualTo(Status.UP);
		assertThat(health.getDetails()).containsEntry("version", "2.8.9");
	}

	@Test
	void redisIsDown() {
		RedisConnection redisConnection = mock(RedisConnection.class);
		RedisServerCommands serverCommands = mock(RedisServerCommands.class);
		given(redisConnection.serverCommands()).willReturn(serverCommands);
		given(serverCommands.info()).willThrow(new RedisConnectionFailureException("Connection failed"));
		DataRedisHealthIndicator healthIndicator = createHealthIndicator(redisConnection);
		Health health = healthIndicator.health();
		assertThat(health.getStatus()).isEqualTo(Status.DOWN);
		assertThat((String) health.getDetails().get("error")).contains("Connection failed");
	}

	@Test
	void healthWhenClusterStateIsAbsentShouldBeUp() {
		RedisConnectionFactory redisConnectionFactory = createClusterConnectionFactory(null);
		DataRedisHealthIndicator healthIndicator = new DataRedisHealthIndicator(redisConnectionFactory);
		Health health = healthIndicator.health();
		assertThat(health.getStatus()).isEqualTo(Status.UP);
		assertThat(health.getDetails()).containsEntry("cluster_size", 4L);
		assertThat(health.getDetails()).containsEntry("slots_up", 4L);
		assertThat(health.getDetails()).containsEntry("slots_fail", 0L);
		then(redisConnectionFactory).should(atLeastOnce()).getConnection();
	}

	@Test
	void healthWhenClusterStateIsOkShouldBeUp() {
		RedisConnectionFactory redisConnectionFactory = createClusterConnectionFactory("ok");
		DataRedisHealthIndicator healthIndicator = new DataRedisHealthIndicator(redisConnectionFactory);
		Health health = healthIndicator.health();
		assertThat(health.getStatus()).isEqualTo(Status.UP);
		assertThat(health.getDetails()).containsEntry("cluster_size", 4L);
		assertThat(health.getDetails()).containsEntry("slots_up", 4L);
		assertThat(health.getDetails()).containsEntry("slots_fail", 0L);
		then(redisConnectionFactory).should(atLeastOnce()).getConnection();
	}

	@Test
	void healthWhenClusterStateIsFailShouldBeDown() {
		RedisConnectionFactory redisConnectionFactory = createClusterConnectionFactory("fail");
		DataRedisHealthIndicator healthIndicator = new DataRedisHealthIndicator(redisConnectionFactory);
		Health health = healthIndicator.health();
		assertThat(health.getStatus()).isEqualTo(Status.DOWN);
		assertThat(health.getDetails()).containsEntry("cluster_size", 4L);
		assertThat(health.getDetails()).containsEntry("slots_up", 3L);
		assertThat(health.getDetails()).containsEntry("slots_fail", 1L);
		then(redisConnectionFactory).should(atLeastOnce()).getConnection();
	}

	private DataRedisHealthIndicator createHealthIndicator(RedisConnection redisConnection) {
		RedisConnectionFactory redisConnectionFactory = mock(RedisConnectionFactory.class);
		given(redisConnectionFactory.getConnection()).willReturn(redisConnection);
		return new DataRedisHealthIndicator(redisConnectionFactory);
	}

	private RedisConnectionFactory createClusterConnectionFactory(@Nullable String state) {
		Properties clusterProperties = new Properties();
		if (state != null) {
			clusterProperties.setProperty("cluster_state", state);
		}
		clusterProperties.setProperty("cluster_size", "4");
		boolean failure = "fail".equals(state);
		clusterProperties.setProperty("cluster_slots_ok", failure ? "3" : "4");
		clusterProperties.setProperty("cluster_slots_fail", failure ? "1" : "0");
		List<RedisClusterNode> redisMasterNodes = Arrays.asList(new RedisClusterNode("127.0.0.1", 7001),
				new RedisClusterNode("127.0.0.2", 7001));
		RedisClusterConnection redisConnection = mock(RedisClusterConnection.class);
		given(redisConnection.clusterGetNodes()).willReturn(redisMasterNodes);
		given(redisConnection.clusterGetClusterInfo()).willReturn(new ClusterInfo(clusterProperties));
		RedisConnectionFactory redisConnectionFactory = mock(RedisConnectionFactory.class);
		given(redisConnectionFactory.getConnection()).willReturn(redisConnection);
		return redisConnectionFactory;
	}

}
