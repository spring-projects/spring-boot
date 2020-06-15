/*
 * Copyright 2012-2020 the original author or authors.
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

package org.springframework.boot.actuate.cassandra;

import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.DriverTimeoutException;
import com.datastax.oss.driver.api.core.cql.ResultSet;
import com.datastax.oss.driver.api.core.cql.Row;
import com.datastax.oss.driver.api.core.cql.SimpleStatement;
import org.junit.jupiter.api.Test;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link CassandraDriverHealthIndicator}.
 *
 * @author Alexandre Dutra
 * @since 2.4.0
 */
class CassandraDriverHealthIndicatorTests {

	@Test
	void createWhenCqlSessionIsNullShouldThrowException() {
		assertThatIllegalArgumentException().isThrownBy(() -> new CassandraDriverHealthIndicator(null));
	}

	@Test
	void healthWithCassandraUp() {
		CqlSession session = mock(CqlSession.class);
		ResultSet resultSet = mock(ResultSet.class);
		Row row = mock(Row.class);
		given(session.execute(any(SimpleStatement.class))).willReturn(resultSet);
		given(resultSet.one()).willReturn(row);
		given(row.isNull(0)).willReturn(false);
		given(row.getString(0)).willReturn("1.0.0");
		CassandraDriverHealthIndicator healthIndicator = new CassandraDriverHealthIndicator(session);
		Health health = healthIndicator.health();
		assertThat(health.getStatus()).isEqualTo(Status.UP);
		assertThat(health.getDetails().get("version")).isEqualTo("1.0.0");
	}

	@Test
	void healthWithCassandraDown() {
		CqlSession session = mock(CqlSession.class);
		given(session.execute(any(SimpleStatement.class))).willThrow(new DriverTimeoutException("Test Exception"));
		CassandraDriverHealthIndicator healthIndicator = new CassandraDriverHealthIndicator(session);
		Health health = healthIndicator.health();
		assertThat(health.getStatus()).isEqualTo(Status.DOWN);
		assertThat(health.getDetails().get("error"))
				.isEqualTo(DriverTimeoutException.class.getName() + ": Test Exception");
	}

}
