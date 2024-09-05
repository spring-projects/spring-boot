/*
 * Copyright 2012-2024 the original author or authors.
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

package org.springframework.boot.testcontainers.service.connection.redis;

import java.util.List;

import com.redis.testcontainers.RedisContainer;
import com.redis.testcontainers.RedisStackContainer;
import org.testcontainers.containers.Container;
import org.testcontainers.containers.GenericContainer;

import org.springframework.boot.autoconfigure.data.redis.RedisConnectionDetails;
import org.springframework.boot.testcontainers.service.connection.ContainerConnectionDetailsFactory;
import org.springframework.boot.testcontainers.service.connection.ContainerConnectionSource;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;

/**
 * {@link ContainerConnectionDetailsFactory} to create {@link RedisConnectionDetails} from
 * a {@link ServiceConnection @ServiceConnection}-annotated {@link GenericContainer} using
 * the {@code "redis"} image.
 *
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 * @author Phillip Webb
 * @author Eddú Meléndez
 */
class RedisContainerConnectionDetailsFactory
		extends ContainerConnectionDetailsFactory<Container<?>, RedisConnectionDetails> {

	private static final List<String> REDIS_IMAGE_NAMES = List.of("redis", "bitnami/redis", "redis/redis-stack",
			"redis/redis-stack-server");

	private static final int REDIS_PORT = 6379;

	RedisContainerConnectionDetailsFactory() {
		super(REDIS_IMAGE_NAMES);
	}

	@Override
	protected boolean sourceAccepts(ContainerConnectionSource<Container<?>> source, Class<?> requiredContainerType,
			Class<?> requiredConnectionDetailsType) {
		return super.sourceAccepts(source, requiredContainerType, requiredConnectionDetailsType)
				|| source.accepts(ANY_CONNECTION_NAME, RedisContainer.class, requiredConnectionDetailsType)
				|| source.accepts(ANY_CONNECTION_NAME, RedisStackContainer.class, requiredConnectionDetailsType);
	}

	@Override
	protected RedisConnectionDetails getContainerConnectionDetails(ContainerConnectionSource<Container<?>> source) {
		return new RedisContainerConnectionDetails(source);
	}

	/**
	 * {@link RedisConnectionDetails} backed by a {@link ContainerConnectionSource}.
	 */
	private static final class RedisContainerConnectionDetails extends ContainerConnectionDetails<Container<?>>
			implements RedisConnectionDetails {

		private RedisContainerConnectionDetails(ContainerConnectionSource<Container<?>> source) {
			super(source);
		}

		@Override
		public Standalone getStandalone() {
			return Standalone.of(getContainer().getHost(), getContainer().getMappedPort(REDIS_PORT));
		}

	}

}
