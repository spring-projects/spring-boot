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

package org.springframework.boot.testcontainers.service.connection.hazelcast;

import java.util.Map;

import com.hazelcast.client.config.ClientConfig;
import org.testcontainers.containers.Container;
import org.testcontainers.containers.GenericContainer;

import org.springframework.boot.autoconfigure.hazelcast.HazelcastConnectionDetails;
import org.springframework.boot.testcontainers.service.connection.ContainerConnectionDetailsFactory;
import org.springframework.boot.testcontainers.service.connection.ContainerConnectionSource;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;

/**
 * {@link ContainerConnectionDetailsFactory} to create {@link HazelcastConnectionDetails}
 * from a {@link ServiceConnection @ServiceConnection}-annotated {@link GenericContainer}
 * using the {@code "hazelcast/hazelcast"} image.
 *
 * @author Dmytro Nosan
 */
class HazelcastContainerConnectionDetailsFactory
		extends ContainerConnectionDetailsFactory<Container<?>, HazelcastConnectionDetails> {

	private static final int DEFAULT_PORT = 5701;

	private static final String CLUSTER_NAME_ENV = "HZ_CLUSTERNAME";

	HazelcastContainerConnectionDetailsFactory() {
		super("hazelcast/hazelcast", "com.hazelcast.client.config.ClientConfig");
	}

	@Override
	protected HazelcastConnectionDetails getContainerConnectionDetails(ContainerConnectionSource<Container<?>> source) {
		return new HazelcastContainerConnectionDetails(source);
	}

	/**
	 * {@link HazelcastConnectionDetails} backed by a {@link ContainerConnectionSource}.
	 */
	private static final class HazelcastContainerConnectionDetails extends ContainerConnectionDetails<Container<?>>
			implements HazelcastConnectionDetails {

		private HazelcastContainerConnectionDetails(ContainerConnectionSource<Container<?>> source) {
			super(source);
		}

		@Override
		public ClientConfig getClientConfig() {
			ClientConfig config = new ClientConfig();
			Container<?> container = getContainer();
			Map<String, String> env = container.getEnvMap();
			String clusterName = env.get(CLUSTER_NAME_ENV);
			if (clusterName != null) {
				config.setClusterName(clusterName);
			}
			config.getNetworkConfig().addAddress(container.getHost() + ":" + container.getMappedPort(DEFAULT_PORT));
			return config;
		}

	}

}
