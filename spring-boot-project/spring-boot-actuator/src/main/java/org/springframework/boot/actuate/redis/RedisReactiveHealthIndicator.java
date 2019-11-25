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

import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import org.springframework.boot.actuate.health.AbstractReactiveHealthIndicator;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.ReactiveHealthIndicator;
import org.springframework.data.redis.connection.ReactiveRedisConnection;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;

/**
 * A {@link ReactiveHealthIndicator} for Redis.
 *
 * @author Stephane Nicoll
 * @author Mark Paluch
 * @author Artsiom Yudovin
 * @since 2.0.0
 */
public class RedisReactiveHealthIndicator extends AbstractReactiveHealthIndicator {

	private final ReactiveRedisConnectionFactory connectionFactory;

	public RedisReactiveHealthIndicator(ReactiveRedisConnectionFactory connectionFactory) {
		super("Redis health check failed");
		this.connectionFactory = connectionFactory;
	}

	@Override
	protected Mono<Health> doHealthCheck(Health.Builder builder) {
		return getConnection().flatMap((connection) -> doHealthCheck(builder, connection));
	}

	private Mono<Health> doHealthCheck(Health.Builder builder, ReactiveRedisConnection connection) {
		return connection.serverCommands().info().map((info) -> up(builder, info))
				.onErrorResume((ex) -> Mono.just(down(builder, ex)))
				.flatMap((health) -> connection.closeLater().thenReturn(health));
	}

	private Mono<ReactiveRedisConnection> getConnection() {
		return Mono.fromSupplier(this.connectionFactory::getReactiveConnection)
				.subscribeOn(Schedulers.boundedElastic());
	}

	private Health up(Health.Builder builder, Properties info) {
		return builder.up()
				.withDetail(RedisHealthIndicator.VERSION, info.getProperty(RedisHealthIndicator.REDIS_VERSION)).build();
	}

	private Health down(Health.Builder builder, Throwable cause) {
		return builder.down(cause).build();
	}

}
