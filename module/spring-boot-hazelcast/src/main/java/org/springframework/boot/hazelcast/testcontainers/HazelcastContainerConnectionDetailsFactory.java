/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.hazelcast.testcontainers;

import java.util.Map;

import com.hazelcast.client.config.ClientConfig;
import org.testcontainers.containers.Container;
import org.testcontainers.containers.GenericContainer;

import org.springframework.boot.hazelcast.autoconfigure.HazelcastConnectionDetails;
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
