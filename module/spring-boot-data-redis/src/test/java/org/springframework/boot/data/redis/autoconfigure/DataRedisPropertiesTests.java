/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.data.redis.autoconfigure;

import io.lettuce.core.cluster.ClusterTopologyRefreshOptions;
import org.junit.jupiter.api.Test;

import org.springframework.boot.data.redis.autoconfigure.DataRedisProperties.Lettuce;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link DataRedisProperties}.
 *
 * @author Stephane Nicoll
 */
class DataRedisPropertiesTests {

	@Test
	void lettuceDefaultsAreConsistent() {
		Lettuce lettuce = new DataRedisProperties().getLettuce();
		ClusterTopologyRefreshOptions defaultClusterTopologyRefreshOptions = ClusterTopologyRefreshOptions.builder()
			.build();
		assertThat(lettuce.getCluster().getRefresh().isDynamicRefreshSources())
			.isEqualTo(defaultClusterTopologyRefreshOptions.useDynamicRefreshSources());
	}

	@Test
	void sslIsNotEnabledWhenBundleIsEmpty() {
		DataRedisProperties properties = new DataRedisProperties();
		properties.getSsl().setBundle("");
		assertThat(properties.getSsl().isEnabled()).isFalse();
	}

}
