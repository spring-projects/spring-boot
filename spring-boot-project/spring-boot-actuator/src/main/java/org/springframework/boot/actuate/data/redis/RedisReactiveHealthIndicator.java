/*
 * Copyright 2012-2023 the original author or authors.
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

import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import org.springframework.boot.actuate.health.AbstractReactiveHealthIndicator;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.ReactiveHealthIndicator;
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
 * @since 2.0.0
 */
public class RedisReactiveHealthIndicator extends AbstractReactiveHealthIndicator {

	private final ReactiveRedisConnectionFactory connectionFactory;

	/**
     * Constructs a new RedisReactiveHealthIndicator with the specified ReactiveRedisConnectionFactory.
     * 
     * @param connectionFactory the ReactiveRedisConnectionFactory to be used for health check
     */
    public RedisReactiveHealthIndicator(ReactiveRedisConnectionFactory connectionFactory) {
		super("Redis health check failed");
		this.connectionFactory = connectionFactory;
	}

	/**
     * Performs a health check on the Redis connection.
     *
     * @param builder the Health.Builder used to build the health status
     * @return a Mono emitting the health status
     */
    @Override
	protected Mono<Health> doHealthCheck(Health.Builder builder) {
		return getConnection().flatMap((connection) -> doHealthCheck(builder, connection));
	}

	/**
     * Retrieves a reactive Redis connection from the connection factory.
     *
     * @return a Mono emitting the reactive Redis connection
     */
    private Mono<ReactiveRedisConnection> getConnection() {
		return Mono.fromSupplier(this.connectionFactory::getReactiveConnection)
			.subscribeOn(Schedulers.boundedElastic());
	}

	/**
     * Performs a health check on the Redis connection.
     * 
     * @param builder the Health.Builder object used to build the health status
     * @param connection the ReactiveRedisConnection object representing the Redis connection
     * @return a Mono object that emits the Health status
     */
    private Mono<Health> doHealthCheck(Health.Builder builder, ReactiveRedisConnection connection) {
		return getHealth(builder, connection).onErrorResume((ex) -> Mono.just(builder.down(ex).build()))
			.flatMap((health) -> connection.closeLater().thenReturn(health));
	}

	/**
     * Retrieves the health status of the Redis server.
     *
     * @param builder The Health.Builder object used to build the Health instance.
     * @param connection The ReactiveRedisConnection object used to interact with the Redis server.
     * @return A Mono<Health> object representing the health status of the Redis server.
     */
    private Mono<Health> getHealth(Health.Builder builder, ReactiveRedisConnection connection) {
		if (connection instanceof ReactiveRedisClusterConnection clusterConnection) {
			return clusterConnection.clusterGetClusterInfo().map((info) -> fromClusterInfo(builder, info));
		}
		return connection.serverCommands().info("server").map((info) -> up(builder, info));
	}

	/**
     * Returns an instance of Health indicating that the Redis health check is up.
     * 
     * @param builder the Health.Builder object used to build the Health instance
     * @param info the Properties object containing additional information about the health check
     * @return an instance of Health indicating that the Redis health check is up
     */
    private Health up(Health.Builder builder, Properties info) {
		return RedisHealth.up(builder, info).build();
	}

	/**
     * Retrieves the health information from the given cluster information and builds a {@link Health} object.
     *
     * @param builder     the builder used to construct the {@link Health} object
     * @param clusterInfo the cluster information used to retrieve the health information
     * @return the {@link Health} object containing the health information
     */
    private Health fromClusterInfo(Health.Builder builder, ClusterInfo clusterInfo) {
		return RedisHealth.fromClusterInfo(builder, clusterInfo).build();
	}

}
