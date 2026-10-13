/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.cassandra.docker.compose;

import java.util.Map;

import org.jspecify.annotations.Nullable;

/**
 * Cassandra environment details.
 *
 * @author Scott Frederick
 */
class CassandraEnvironment {

	private final String datacenter;

	CassandraEnvironment(Map<String, @Nullable String> env) {
		this.datacenter = getDatacenter(env);
	}

	private static String getDatacenter(Map<String, @Nullable String> env) {
		String datacenter = env.getOrDefault("CASSANDRA_DC", env.get("CASSANDRA_DATACENTER"));
		return (datacenter != null) ? datacenter : "datacenter1";
	}

	String getDatacenter() {
		return this.datacenter;
	}

}
