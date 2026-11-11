/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.actuate.info;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link InfoEndpoint}.
 *
 * @author Phillip Webb
 * @author Dave Syer
 * @author Meang Akira Tanaka
 * @author Andy Wilkinson
 */
class InfoEndpointTests {

	@Test
	void info() {
		InfoEndpoint endpoint = new InfoEndpoint(Arrays.asList((builder) -> builder.withDetail("key1", "value1"),
				(builder) -> builder.withDetail("key2", "value2")));
		Map<String, Object> info = endpoint.info();
		assertThat(info).hasSize(2);
		assertThat(info).containsEntry("key1", "value1");
		assertThat(info).containsEntry("key2", "value2");
	}

	@Test
	void infoWithNoContributorsProducesEmptyMap() {
		InfoEndpoint endpoint = new InfoEndpoint(Collections.emptyList());
		Map<String, Object> info = endpoint.info();
		assertThat(info).isEmpty();
	}

}
