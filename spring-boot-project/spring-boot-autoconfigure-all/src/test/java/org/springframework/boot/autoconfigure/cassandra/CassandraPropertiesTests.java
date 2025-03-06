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

package org.springframework.boot.autoconfigure.cassandra;

import java.time.Duration;

import com.datastax.oss.driver.api.core.config.OptionsMap;
import com.datastax.oss.driver.api.core.config.TypedDriverOption;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link CassandraProperties}.
 *
 * @author Chris Bono
 * @author Stephane Nicoll
 */
class CassandraPropertiesTests {

	/**
	 * To let a configuration file override values, {@link CassandraProperties} can't have
	 * any default hardcoded. This test makes sure that the default that we moved to
	 * manual meta-data are accurate.
	 */
	@Test
	void defaultValuesInManualMetadataAreConsistent() {
		OptionsMap driverDefaults = OptionsMap.driverDefaults();
		// spring.cassandra.connection.connect-timeout
		assertThat(driverDefaults.get(TypedDriverOption.CONNECTION_CONNECT_TIMEOUT)).isEqualTo(Duration.ofSeconds(5));
		// spring.cassandra.connection.init-query-timeout
		assertThat(driverDefaults.get(TypedDriverOption.CONNECTION_INIT_QUERY_TIMEOUT))
			.isEqualTo(Duration.ofSeconds(5));
		// spring.cassandra.request.timeout
		assertThat(driverDefaults.get(TypedDriverOption.REQUEST_TIMEOUT)).isEqualTo(Duration.ofSeconds(2));
		// spring.cassandra.request.page-size
		assertThat(driverDefaults.get(TypedDriverOption.REQUEST_PAGE_SIZE)).isEqualTo(5000);
		// spring.cassandra.request.throttler.type
		assertThat(driverDefaults.get(TypedDriverOption.REQUEST_THROTTLER_CLASS))
			.isEqualTo("PassThroughRequestThrottler"); // "none"
		// spring.cassandra.pool.heartbeat-interval
		assertThat(driverDefaults.get(TypedDriverOption.HEARTBEAT_INTERVAL)).isEqualTo(Duration.ofSeconds(30));
		// spring.cassandra.pool.idle-timeout
		assertThat(driverDefaults.get(TypedDriverOption.HEARTBEAT_TIMEOUT)).isEqualTo(Duration.ofSeconds(5));
	}

}
