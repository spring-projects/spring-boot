/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.neo4j.autoconfigure;

import java.time.Duration;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.neo4j.driver.Config;
import org.neo4j.driver.internal.retry.RetrySettings;

import org.springframework.boot.neo4j.autoconfigure.Neo4jProperties.Pool;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link Neo4jProperties}.
 *
 * @author Michael J. Simons
 * @author Stephane Nicoll
 */
class Neo4jPropertiesTests {

	@Test
	void poolSettingsHaveConsistentDefaults() {
		Config defaultConfig = Config.defaultConfig();
		Pool pool = new Neo4jProperties().getPool();
		assertThat(pool.isLogLeakedSessions()).isEqualTo(defaultConfig.logLeakedSessions());
		assertThat(pool.getMaxConnectionPoolSize()).isEqualTo(defaultConfig.maxConnectionPoolSize());
		assertDuration(pool.getIdleTimeBeforeConnectionTest(), defaultConfig.idleTimeBeforeConnectionTest());
		assertDuration(pool.getMaxConnectionLifetime(), defaultConfig.maxConnectionLifetimeMillis());
		assertDuration(pool.getConnectionAcquisitionTimeout(), defaultConfig.connectionAcquisitionTimeoutMillis());
	}

	@Test
	void securitySettingsHaveConsistentDefaults() {
		Config defaultConfig = Config.defaultConfig();
		Neo4jProperties properties = new Neo4jProperties();
		assertThat(properties.getSecurity().isEncrypted()).isEqualTo(defaultConfig.encrypted());
		assertThat(properties.getSecurity().getTrustStrategy().name())
			.isEqualTo(defaultConfig.trustStrategy().strategy().name());
		assertThat(properties.getSecurity().isHostnameVerificationEnabled())
			.isEqualTo(defaultConfig.trustStrategy().isHostnameVerificationEnabled());
	}

	@Test
	void driverSettingsHaveConsistentDefaults() {
		Config defaultConfig = Config.defaultConfig();
		Neo4jProperties properties = new Neo4jProperties();
		assertDuration(properties.getConnectionTimeout(), defaultConfig.connectionTimeoutMillis());
		assertDuration(properties.getMaxTransactionRetryTime(), RetrySettings.DEFAULT.maxRetryTimeMs());
	}

	private static void assertDuration(@Nullable Duration duration, long expectedValueInMillis) {
		if (expectedValueInMillis == -1) {
			assertThat(duration).isNull();
		}
		else {
			assertThat(duration).isNotNull();
			assertThat(duration.toMillis()).isEqualTo(expectedValueInMillis);
		}
	}

}
