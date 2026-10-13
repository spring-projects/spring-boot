/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.hazelcast.docker.compose;

import java.util.Collections;
import java.util.Map;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link HazelcastEnvironment}.
 *
 * @author Dmytro Nosan
 */
class HazelcastEnvironmentTests {

	@Test
	void getClusterNameWhenHasNoHzClusterNameSet() {
		HazelcastEnvironment environment = new HazelcastEnvironment(Collections.emptyMap());
		assertThat(environment.getClusterName()).isNull();
	}

	@Test
	void getClusterNameWhenHzClusterNameSet() {
		HazelcastEnvironment environment = new HazelcastEnvironment(Map.of("HZ_CLUSTERNAME", "spring-boot"));
		assertThat(environment.getClusterName()).isEqualTo("spring-boot");
	}

}
