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
		String failMsg = "the default value has diverged from the driver's built-in default";

		CassandraProperties properties = new CassandraProperties();
		OptionsMap optionsMap = OptionsMap.driverDefaults();

		assertThat(properties.getConnection().getConnectTimeout()).describedAs(failMsg)
				.isEqualTo(optionsMap.get(TypedDriverOption.CONNECTION_CONNECT_TIMEOUT));

		assertThat(properties.getConnection().getInitQueryTimeout()).describedAs(failMsg)
				.isEqualTo(optionsMap.get(TypedDriverOption.CONNECTION_INIT_QUERY_TIMEOUT));

		assertThat(properties.getRequest().getTimeout()).describedAs(failMsg)
				.isEqualTo(optionsMap.get(TypedDriverOption.REQUEST_TIMEOUT));

		assertThat(properties.getRequest().getPageSize()).describedAs(failMsg)
				.isEqualTo(optionsMap.get(TypedDriverOption.REQUEST_PAGE_SIZE));

		assertThat(properties.getRequest().getThrottler().getType().type()).describedAs(failMsg)
				.isEqualTo(optionsMap.get(TypedDriverOption.REQUEST_THROTTLER_CLASS));

		assertThat(properties.getPool().getHeartbeatInterval()).describedAs(failMsg)
				.isEqualTo(optionsMap.get(TypedDriverOption.HEARTBEAT_INTERVAL));
	}

}
