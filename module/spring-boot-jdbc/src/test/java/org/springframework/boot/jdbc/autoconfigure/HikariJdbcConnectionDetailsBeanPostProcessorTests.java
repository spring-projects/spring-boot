/*
 * Copyright 2012-present the original author or authors.
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

package org.springframework.boot.jdbc.autoconfigure;

import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.jdbc.DatabaseDriver;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link HikariJdbcConnectionDetailsBeanPostProcessor}.
 *
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 * @author Phillip Webb
 */
class HikariJdbcConnectionDetailsBeanPostProcessorTests {

	@Test
	@SuppressWarnings("unchecked")
	void setUsernamePasswordAndUrl() {
		HikariDataSource dataSource = new HikariDataSource();
		dataSource.setJdbcUrl("will-be-overwritten");
		dataSource.setUsername("will-be-overwritten");
		dataSource.setPassword("will-be-overwritten");
		dataSource.setDriverClassName(DatabaseDriver.H2.getDriverClassName());
		new HikariJdbcConnectionDetailsBeanPostProcessor(mock(ObjectProvider.class)).processDataSource(dataSource,
				new TestJdbcConnectionDetails());
		assertThat(dataSource.getJdbcUrl()).isEqualTo("jdbc:customdb://customdb.example.com:12345/database-1");
		assertThat(dataSource.getUsername()).isEqualTo("user-1");
		assertThat(dataSource.getPassword()).isEqualTo("password-1");
		assertThat(dataSource.getDriverClassName()).isEqualTo(DatabaseDriver.POSTGRESQL.getDriverClassName());
	}

	@Test
	@SuppressWarnings("unchecked")
	void toleratesConnectionDetailsWithNullDriverClassName() {
		HikariDataSource dataSource = new HikariDataSource();
		dataSource.setDriverClassName(DatabaseDriver.H2.getDriverClassName());
		JdbcConnectionDetails connectionDetails = mock(JdbcConnectionDetails.class);
		new HikariJdbcConnectionDetailsBeanPostProcessor(mock(ObjectProvider.class)).processDataSource(dataSource,
				connectionDetails);
		assertThat(dataSource.getDriverClassName()).isEqualTo(DatabaseDriver.H2.getDriverClassName());
	}

}
