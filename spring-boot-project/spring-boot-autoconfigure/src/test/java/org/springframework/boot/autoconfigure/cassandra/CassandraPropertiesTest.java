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
class CassandraPropertiesTest {

	@Test
	void defaultValuesAreConsistent() {
		CassandraProperties properties = new CassandraProperties();
		OptionsMap optionsMap = OptionsMap.driverDefaults();

		assertThat(properties.getConnection().getConnectTimeout())
				.isEqualTo(optionsMap.get(TypedDriverOption.CONNECTION_CONNECT_TIMEOUT));

		assertThat(properties.getConnection().getInitQueryTimeout())
				.isEqualTo(optionsMap.get(TypedDriverOption.CONNECTION_INIT_QUERY_TIMEOUT));

		assertThat(properties.getRequest().getTimeout()).isEqualTo(optionsMap.get(TypedDriverOption.REQUEST_TIMEOUT));

		assertThat(properties.getRequest().getPageSize())
				.isEqualTo(optionsMap.get(TypedDriverOption.REQUEST_PAGE_SIZE));

		assertThat(properties.getRequest().getThrottler().getType().type())
				.isEqualTo(optionsMap.get(TypedDriverOption.REQUEST_THROTTLER_CLASS));

		assertThat(properties.getPool().getHeartbeatInterval())
				.isEqualTo(optionsMap.get(TypedDriverOption.HEARTBEAT_INTERVAL));
	}

}
