/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.data.redis.docker.compose;

import org.jspecify.annotations.Nullable;

import org.springframework.boot.data.redis.autoconfigure.DataRedisConnectionDetails;
import org.springframework.boot.docker.compose.core.RunningService;
import org.springframework.boot.docker.compose.service.connection.DockerComposeConnectionDetailsFactory;
import org.springframework.boot.docker.compose.service.connection.DockerComposeConnectionSource;
import org.springframework.boot.ssl.SslBundle;

/**
 * {@link DockerComposeConnectionDetailsFactory} to create
 * {@link DataRedisConnectionDetails} for a {@code redis} service.
 *
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 * @author Phillip Webb
 * @author Scott Frederick
 * @author Eddú Meléndez
 */
class RedisDockerComposeConnectionDetailsFactory
		extends DockerComposeConnectionDetailsFactory<DataRedisConnectionDetails> {

	private static final String[] REDIS_CONTAINER_NAMES = { "redis", "redis/redis-stack", "redis/redis-stack-server" };

	private static final int REDIS_PORT = 6379;

	RedisDockerComposeConnectionDetailsFactory() {
		super(REDIS_CONTAINER_NAMES);
	}

	@Override
	protected DataRedisConnectionDetails getDockerComposeConnectionDetails(DockerComposeConnectionSource source) {
		return new RedisDockerComposeConnectionDetails(source.getRunningService());
	}

	/**
	 * {@link DataRedisConnectionDetails} backed by a {@code redis}
	 * {@link RunningService}.
	 */
	static class RedisDockerComposeConnectionDetails extends DockerComposeConnectionDetails
			implements DataRedisConnectionDetails {

		private final Standalone standalone;

		private final @Nullable SslBundle sslBundle;

		RedisDockerComposeConnectionDetails(RunningService service) {
			super(service);
			this.standalone = Standalone.of(service.host(), service.ports().get(REDIS_PORT));
			this.sslBundle = getSslBundle(service);
		}

		@Override
		public @Nullable SslBundle getSslBundle() {
			return this.sslBundle;
		}

		@Override
		public Standalone getStandalone() {
			return this.standalone;
		}

	}

}
