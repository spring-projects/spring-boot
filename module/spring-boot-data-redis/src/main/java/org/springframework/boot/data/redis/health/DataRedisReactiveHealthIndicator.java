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

import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import org.springframework.boot.health.contributor.AbstractReactiveHealthIndicator;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.ReactiveHealthIndicator;
import org.springframework.data.redis.connection.ClusterInfo;
import org.springframework.data.redis.connection.ReactiveRedisClusterConnection;
import org.springframework.data.redis.connection.ReactiveRedisConnection;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;

/**
 * A {@link ReactiveHealthIndicator} for Redis.
 *
 * @author Stephane Nicoll
 * @author Mark Paluch
 * @author Artsiom Yudovin
 * @author Scott Frederick
 * @since 4.0.0
 */
public class DataRedisReactiveHealthIndicator extends AbstractReactiveHealthIndicator {

	private final ReactiveRedisConnectionFactory connectionFactory;

	public DataRedisReactiveHealthIndicator(ReactiveRedisConnectionFactory connectionFactory) {
		super("Redis health check failed");
		this.connectionFactory = connectionFactory;
	}

	@Override
	protected Mono<Health> doHealthCheck(Health.Builder builder) {
		return getConnection().flatMap((connection) -> doHealthCheck(builder, connection));
	}

	private Mono<ReactiveRedisConnection> getConnection() {
		return Mono.fromSupplier(this.connectionFactory::getReactiveConnection)
			.subscribeOn(Schedulers.boundedElastic());
	}

	private Mono<Health> doHealthCheck(Health.Builder builder, ReactiveRedisConnection connection) {
		return getHealth(builder, connection).onErrorResume((ex) -> Mono.just(builder.down(ex).build()))
			.flatMap((health) -> connection.closeLater().thenReturn(health));
	}

	private Mono<Health> getHealth(Health.Builder builder, ReactiveRedisConnection connection) {
		if (connection instanceof ReactiveRedisClusterConnection clusterConnection) {
			return clusterConnection.clusterGetClusterInfo().map((info) -> fromClusterInfo(builder, info));
		}
		return connection.serverCommands().info("server").map((info) -> up(builder, info));
	}

	private Health up(Health.Builder builder, Properties info) {
		return DataRedisHealth.up(builder, info).build();
	}

	private Health fromClusterInfo(Health.Builder builder, ClusterInfo clusterInfo) {
		return DataRedisHealth.fromClusterInfo(builder, clusterInfo).build();
	}

}
