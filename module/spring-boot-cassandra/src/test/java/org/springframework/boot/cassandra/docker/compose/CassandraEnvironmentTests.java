/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.cassandra.docker.compose;

import java.util.Collections;
import java.util.Map;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link CassandraEnvironment}.
 *
 * @author Scott Frederick
 */
class CassandraEnvironmentTests {

	@Test
	void getDatacenterWhenDatacenterIsNotSet() {
		CassandraEnvironment environment = new CassandraEnvironment(Collections.emptyMap());
		assertThat(environment.getDatacenter()).isEqualTo("datacenter1");
	}

	@Test
	void getDatacenterWhenDcIsSet() {
		CassandraEnvironment environment = new CassandraEnvironment(Map.of("CASSANDRA_DC", "testdc1"));
		assertThat(environment.getDatacenter()).isEqualTo("testdc1");
	}

	@Test
	void getDatacenterWhenDatacenterIsSet() {
		CassandraEnvironment environment = new CassandraEnvironment(Map.of("CASSANDRA_DATACENTER", "testdc1"));
		assertThat(environment.getDatacenter()).isEqualTo("testdc1");
	}

}
