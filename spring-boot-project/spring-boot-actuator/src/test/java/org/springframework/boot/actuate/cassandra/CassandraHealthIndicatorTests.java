/*
 * Copyright 2012-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.actuate.cassandra;

import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.querybuilder.Select;
import org.junit.Test;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;
import org.springframework.data.cassandra.core.CassandraOperations;
import org.springframework.data.cassandra.core.cql.CqlOperations;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link CassandraHealthIndicator}.
 *
 * @author Oleksii Bondar
 */
public class CassandraHealthIndicatorTests {

	@Test
	public void createWhenCassandraOperationsIsNullShouldThrowException() {
		assertThatIllegalArgumentException()
				.isThrownBy(() -> new CassandraHealthIndicator(null));
	}

	@Test
	public void verifyHealthStatusWhenExhausted() {
		CassandraOperations cassandraOperations = mock(CassandraOperations.class);
		CqlOperations cqlOperations = mock(CqlOperations.class);
		ResultSet resultSet = mock(ResultSet.class);
		CassandraHealthIndicator healthIndicator = new CassandraHealthIndicator(
				cassandraOperations);
		given(cassandraOperations.getCqlOperations()).willReturn(cqlOperations);
		given(cqlOperations.queryForResultSet(any(Select.class))).willReturn(resultSet);
		given(resultSet.isExhausted()).willReturn(true);
		Health health = healthIndicator.health();
		assertThat(health.getStatus()).isEqualTo(Status.UP);
	}

	@Test
	public void verifyHealthStatusWithVersion() {
		CassandraOperations cassandraOperations = mock(CassandraOperations.class);
		CqlOperations cqlOperations = mock(CqlOperations.class);
		ResultSet resultSet = mock(ResultSet.class);
		Row row = mock(Row.class);
		CassandraHealthIndicator healthIndicator = new CassandraHealthIndicator(
				cassandraOperations);
		given(cassandraOperations.getCqlOperations()).willReturn(cqlOperations);
		given(cqlOperations.queryForResultSet(any(Select.class))).willReturn(resultSet);
		given(resultSet.isExhausted()).willReturn(false);
		given(resultSet.one()).willReturn(row);
		String expectedVersion = "1.0.0";
		given(row.getString(0)).willReturn(expectedVersion);
		Health health = healthIndicator.health();
		assertThat(health.getStatus()).isEqualTo(Status.UP);
		assertThat(health.getDetails().get("version")).isEqualTo(expectedVersion);
	}

}
