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

package org.springframework.boot.testcontainers.service.connection.redis;

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
 */
class RedisContainerConnectionDetailsFactory
		extends ContainerConnectionDetailsFactory<Container<?>, RedisConnectionDetails> {

	/**
	 * Constructs a new RedisContainerConnectionDetailsFactory object.
	 * @param name the name of the Redis container
	 */
	RedisContainerConnectionDetailsFactory() {
		super("redis");
	}

	/**
	 * Returns the Redis connection details for the specified container connection source.
	 * @param source the container connection source
	 * @return the Redis connection details
	 */
	@Override
	public RedisConnectionDetails getContainerConnectionDetails(ContainerConnectionSource<Container<?>> source) {
		return new RedisContainerConnectionDetails(source);
	}

	/**
	 * {@link RedisConnectionDetails} backed by a {@link ContainerConnectionSource}.
	 */
	private static final class RedisContainerConnectionDetails extends ContainerConnectionDetails<Container<?>>
			implements RedisConnectionDetails {

		/**
		 * Constructs a new RedisContainerConnectionDetails object with the specified
		 * ContainerConnectionSource.
		 * @param source the ContainerConnectionSource used to create the
		 * RedisContainerConnectionDetails object
		 */
		private RedisContainerConnectionDetails(ContainerConnectionSource<Container<?>> source) {
			super(source);
		}

		/**
		 * Returns a Standalone instance representing the connection details of the Redis
		 * container. The Standalone instance contains the host and the first mapped port
		 * of the container.
		 * @return a Standalone instance representing the connection details of the Redis
		 * container
		 */
		@Override
		public Standalone getStandalone() {
			return Standalone.of(getContainer().getHost(), getContainer().getFirstMappedPort());
		}

	}

}
