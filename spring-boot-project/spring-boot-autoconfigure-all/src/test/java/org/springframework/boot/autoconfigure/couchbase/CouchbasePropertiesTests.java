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

package org.springframework.boot.autoconfigure.couchbase;

import com.couchbase.client.core.env.IoConfig;
import com.couchbase.client.core.env.TimeoutConfig;
import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.couchbase.CouchbaseProperties.Io;
import org.springframework.boot.autoconfigure.couchbase.CouchbaseProperties.Timeouts;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link CouchbaseProperties}.
 *
 * @author Stephane Nicoll
 */
class CouchbasePropertiesTests {

	@Test
	void ioHaveConsistentDefaults() {
		Io io = new CouchbaseProperties().getEnv().getIo();
		assertThat(io.getMinEndpoints()).isOne();
		assertThat(io.getMaxEndpoints()).isEqualTo(IoConfig.DEFAULT_MAX_HTTP_CONNECTIONS);
		assertThat(io.getIdleHttpConnectionTimeout()).isEqualTo(IoConfig.DEFAULT_IDLE_HTTP_CONNECTION_TIMEOUT);
	}

	@Test
	void timeoutsHaveConsistentDefaults() {
		Timeouts timeouts = new CouchbaseProperties().getEnv().getTimeouts();
		assertThat(timeouts.getConnect()).isEqualTo(TimeoutConfig.DEFAULT_CONNECT_TIMEOUT);
		assertThat(timeouts.getDisconnect()).isEqualTo(TimeoutConfig.DEFAULT_DISCONNECT_TIMEOUT);
		assertThat(timeouts.getKeyValue()).isEqualTo(TimeoutConfig.DEFAULT_KV_TIMEOUT);
		assertThat(timeouts.getKeyValueDurable()).isEqualTo(TimeoutConfig.DEFAULT_KV_DURABLE_TIMEOUT);
		assertThat(timeouts.getQuery()).isEqualTo(TimeoutConfig.DEFAULT_QUERY_TIMEOUT);
		assertThat(timeouts.getView()).isEqualTo(TimeoutConfig.DEFAULT_VIEW_TIMEOUT);
		assertThat(timeouts.getSearch()).isEqualTo(TimeoutConfig.DEFAULT_SEARCH_TIMEOUT);
		assertThat(timeouts.getAnalytics()).isEqualTo(TimeoutConfig.DEFAULT_ANALYTICS_TIMEOUT);
		assertThat(timeouts.getManagement()).isEqualTo(TimeoutConfig.DEFAULT_MANAGEMENT_TIMEOUT);
	}

}
