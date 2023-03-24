/*
 * Copyright 2012-2023 the original author or authors.
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

package org.springframework.boot.autoconfigure.neo4j;

import java.time.Duration;

import org.junit.jupiter.api.Test;
import org.neo4j.driver.Config;
import org.neo4j.driver.internal.retry.RetrySettings;

import org.springframework.boot.autoconfigure.neo4j.Neo4jProperties.Pool;

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
		assertThat(pool.isMetricsEnabled()).isEqualTo(defaultConfig.isMetricsEnabled());
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

	private static void assertDuration(Duration duration, long expectedValueInMillis) {
		if (expectedValueInMillis == org.neo4j.driver.internal.async.pool.PoolSettings.NOT_CONFIGURED) {
			assertThat(duration).isNull();
		}
		else {
			assertThat(duration.toMillis()).isEqualTo(expectedValueInMillis);
		}
	}

}
