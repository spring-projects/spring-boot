/*
 * Copyright 2012-2019 the original author or authors.
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

package org.springframework.boot.actuate.redis;

import java.util.Properties;

import io.lettuce.core.RedisConnectionException;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.connection.ReactiveClusterServerCommands;
import org.springframework.data.redis.connection.ReactiveRedisClusterConnection;
import org.springframework.data.redis.connection.ReactiveRedisConnection;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.connection.ReactiveServerCommands;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Tests for {@link RedisReactiveHealthIndicator}.
 *
 * @author Stephane Nicoll
 * @author Mark Paluch
 * @author Nikolay Rybak
 * @author Artsiom Yudovin
 * @author Scott Frederick
 */
class RedisReactiveHealthIndicatorTests {

	@Test
	void redisIsUp() {
		Properties info = new Properties();
		info.put("redis_version", "2.8.9");
		ReactiveRedisConnection redisConnection = mock(ReactiveRedisConnection.class);
		given(redisConnection.closeLater()).willReturn(Mono.empty());
		ReactiveServerCommands commands = mock(ReactiveServerCommands.class);
		given(commands.info()).willReturn(Mono.just(info));
		RedisReactiveHealthIndicator healthIndicator = createHealthIndicator(redisConnection, commands);
		Mono<Health> health = healthIndicator.health();
		StepVerifier.create(health).consumeNextWith((h) -> {
			assertThat(h.getStatus()).isEqualTo(Status.UP);
			assertThat(h.getDetails()).containsOnlyKeys("version");
			assertThat(h.getDetails().get("version")).isEqualTo("2.8.9");
		}).verifyComplete();
		verify(redisConnection).closeLater();
	}

	@Test
	void redisClusterIsUp() {
		Properties info = new Properties();
		info.put("127.0.0.1:7002.redis_version", "2.8.9");
		ReactiveRedisConnection redisConnection = mock(ReactiveRedisClusterConnection.class);
		given(redisConnection.closeLater()).willReturn(Mono.empty());
		ReactiveClusterServerCommands commands = mock(ReactiveClusterServerCommands.class);
		given(commands.info()).willReturn(Mono.just(info));
		RedisReactiveHealthIndicator healthIndicator = createHealthIndicator(redisConnection, commands);
		Mono<Health> health = healthIndicator.health();
		StepVerifier.create(health).consumeNextWith((h) -> {
			assertThat(h.getStatus()).isEqualTo(Status.UP);
			assertThat(h.getDetails()).containsOnlyKeys("version");
			assertThat(h.getDetails().get("version")).isEqualTo("2.8.9");
		}).verifyComplete();
		verify(redisConnection).closeLater();
	}

	@Test
	void redisCommandIsDown() {
		ReactiveServerCommands commands = mock(ReactiveServerCommands.class);
		given(commands.info()).willReturn(Mono.error(new RedisConnectionFailureException("Connection failed")));
		ReactiveRedisConnection redisConnection = mock(ReactiveRedisConnection.class);
		given(redisConnection.closeLater()).willReturn(Mono.empty());
		RedisReactiveHealthIndicator healthIndicator = createHealthIndicator(redisConnection, commands);
		Mono<Health> health = healthIndicator.health();
		StepVerifier.create(health).consumeNextWith((h) -> assertThat(h.getStatus()).isEqualTo(Status.DOWN))
				.verifyComplete();
		verify(redisConnection).closeLater();
	}

	@Test
	void redisConnectionIsDown() {
		ReactiveRedisConnectionFactory redisConnectionFactory = mock(ReactiveRedisConnectionFactory.class);
		given(redisConnectionFactory.getReactiveConnection())
				.willThrow(new RedisConnectionException("Unable to connect to localhost:6379"));
		RedisReactiveHealthIndicator healthIndicator = new RedisReactiveHealthIndicator(redisConnectionFactory);
		Mono<Health> health = healthIndicator.health();
		StepVerifier.create(health).consumeNextWith((h) -> assertThat(h.getStatus()).isEqualTo(Status.DOWN))
				.verifyComplete();
	}

	private RedisReactiveHealthIndicator createHealthIndicator(ReactiveRedisConnection redisConnection,
			ReactiveServerCommands serverCommands) {

		ReactiveRedisConnectionFactory redisConnectionFactory = mock(ReactiveRedisConnectionFactory.class);
		given(redisConnectionFactory.getReactiveConnection()).willReturn(redisConnection);
		given(redisConnection.serverCommands()).willReturn(serverCommands);
		return new RedisReactiveHealthIndicator(redisConnectionFactory);
	}

}
