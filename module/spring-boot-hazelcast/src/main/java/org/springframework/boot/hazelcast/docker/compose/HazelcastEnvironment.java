/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.hazelcast.docker.compose;

import java.util.Map;

import org.jspecify.annotations.Nullable;

/**
 * Hazelcast environment details.
 *
 * @author Dmytro Nosan
 */
class HazelcastEnvironment {

	private final @Nullable String clusterName;

	HazelcastEnvironment(Map<String, @Nullable String> env) {
		this.clusterName = env.get("HZ_CLUSTERNAME");
	}

	@Nullable String getClusterName() {
		return this.clusterName;
	}

}
