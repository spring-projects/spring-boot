/*
 * Copyright 2012-2021 the original author or authors.
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

import com.datastax.oss.driver.api.core.config.OptionsMap;
import com.datastax.oss.driver.api.core.config.TypedDriverOption;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link CassandraProperties}.
 *
 * @author Chris Bono
 */
class CassandraPropertiesTests {

	@Test
	void defaultValuesAreConsistent() {
		CassandraProperties properties = new CassandraProperties();
		OptionsMap driverDefaults = OptionsMap.driverDefaults();
		assertThat(properties.getConnection().getConnectTimeout())
				.isEqualTo(driverDefaults.get(TypedDriverOption.CONNECTION_CONNECT_TIMEOUT));
		assertThat(properties.getConnection().getInitQueryTimeout())
				.isEqualTo(driverDefaults.get(TypedDriverOption.CONNECTION_INIT_QUERY_TIMEOUT));
		assertThat(properties.getRequest().getTimeout())
				.isEqualTo(driverDefaults.get(TypedDriverOption.REQUEST_TIMEOUT));
		assertThat(properties.getRequest().getPageSize())
				.isEqualTo(driverDefaults.get(TypedDriverOption.REQUEST_PAGE_SIZE));
		assertThat(properties.getRequest().getThrottler().getType().type())
				.isEqualTo(driverDefaults.get(TypedDriverOption.REQUEST_THROTTLER_CLASS));
		assertThat(properties.getPool().getHeartbeatInterval())
				.isEqualTo(driverDefaults.get(TypedDriverOption.HEARTBEAT_INTERVAL));
		assertThat(properties.getPool().getIdleTimeout())
				.isEqualTo(driverDefaults.get(TypedDriverOption.HEARTBEAT_TIMEOUT));
	}

}
