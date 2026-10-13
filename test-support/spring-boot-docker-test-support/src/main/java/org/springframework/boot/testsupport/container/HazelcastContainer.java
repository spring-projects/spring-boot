/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.testsupport.container;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * A {@link GenericContainer} for Hazelcast.
 *
 * @author Dmytro Nosan
 */
public final class HazelcastContainer extends GenericContainer<HazelcastContainer> {

	private static final int DEFAULT_PORT = 5701;

	public HazelcastContainer(DockerImageName dockerImageName) {
		super(dockerImageName);
		addExposedPorts(DEFAULT_PORT);
	}

	/**
	 * Sets the cluster name.
	 * @param clusterName the cluster name
	 * @return this instance
	 */
	public HazelcastContainer withClusterName(String clusterName) {
		return withEnv("HZ_CLUSTERNAME", clusterName);
	}

}
