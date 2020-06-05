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

import com.datastax.driver.core.Statement;
import org.junit.jupiter.api.Test;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;
import org.springframework.data.cassandra.CassandraInternalException;
import org.springframework.data.cassandra.core.CassandraOperations;
import org.springframework.data.cassandra.core.cql.CqlOperations;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link CassandraHealthIndicator}.
 *
 * @author Oleksii Bondar
 * @author Stephane Nicoll
 */
class CassandraHealthIndicatorTests {

	@Test
	void createWhenCassandraOperationsIsNullShouldThrowException() {
		assertThatIllegalArgumentException().isThrownBy(() -> new CassandraHealthIndicator(null));
	}

	@Test
	void healthWithCassandraUp() {
		CassandraOperations cassandraOperations = mock(CassandraOperations.class);
		CqlOperations cqlOperations = mock(CqlOperations.class);
		CassandraHealthIndicator healthIndicator = new CassandraHealthIndicator(cassandraOperations);
		given(cassandraOperations.getCqlOperations()).willReturn(cqlOperations);
		given(cqlOperations.queryForObject(any(Statement.class), eq(String.class))).willReturn("1.0.0");
		Health health = healthIndicator.health();
		assertThat(health.getStatus()).isEqualTo(Status.UP);
		assertThat(health.getDetails().get("version")).isEqualTo("1.0.0");
	}

	@Test
	void healthWithCassandraDown() {
		CassandraOperations cassandraOperations = mock(CassandraOperations.class);
		given(cassandraOperations.getCqlOperations()).willThrow(new CassandraInternalException("Connection failed"));
		CassandraHealthIndicator healthIndicator = new CassandraHealthIndicator(cassandraOperations);
		Health health = healthIndicator.health();
		assertThat(health.getStatus()).isEqualTo(Status.DOWN);
		assertThat(health.getDetails().get("error"))
				.isEqualTo(CassandraInternalException.class.getName() + ": Connection failed");
	}

}
