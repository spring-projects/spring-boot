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

package org.springframework.boot.test.autoconfigure.data.redis;

import org.testcontainers.containers.GenericContainer;

import org.springframework.boot.autoconfigure.data.redis.RedisConnectionDetails;
import org.springframework.boot.test.autoconfigure.service.connection.ContainerConnectionDetailsFactory;
import org.springframework.boot.test.autoconfigure.service.connection.ContainerConnectionSource;

/**
 * {@link ContainerConnectionDetailsFactory} for
 * {@link RedisServiceConnection @RedisServiceConnection}-annotated
 * {@link GenericContainer} fields.
 *
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 * @author Phillip Webb
 */
class RedisContainerConnectionDetailsFactory
		extends ContainerConnectionDetailsFactory<RedisServiceConnection, RedisConnectionDetails, GenericContainer<?>> {

	@Override
	public RedisConnectionDetails getContainerConnectionDetails(
			ContainerConnectionSource<RedisServiceConnection, RedisConnectionDetails, GenericContainer<?>> source) {
		return new RedisContainerConnectionDetails(source);
	}

	/**
	 * {@link RedisConnectionDetails} backed by a {@link ContainerConnectionSource}.
	 */
	private static final class RedisContainerConnectionDetails extends ContainerConnectionDetails
			implements RedisConnectionDetails {

		private final Standalone standalone;

		private RedisContainerConnectionDetails(
				ContainerConnectionSource<RedisServiceConnection, RedisConnectionDetails, GenericContainer<?>> source) {
			super(source);
			this.standalone = new Standalone() {

				@Override
				public String getHost() {
					return source.getContainer().getHost();
				}

				@Override
				public int getPort() {
					return source.getContainer().getFirstMappedPort();
				}

			};
		}

		@Override
		public Standalone getStandalone() {
			return this.standalone;
		}

	}

}
