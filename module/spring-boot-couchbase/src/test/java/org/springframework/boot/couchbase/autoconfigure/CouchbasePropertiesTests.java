/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.couchbase.autoconfigure;

import com.couchbase.client.core.env.IoConfig;
import com.couchbase.client.core.env.TimeoutConfig;
import org.junit.jupiter.api.Test;

import org.springframework.boot.couchbase.autoconfigure.CouchbaseProperties.Io;
import org.springframework.boot.couchbase.autoconfigure.CouchbaseProperties.Timeouts;

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
