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

package org.springframework.boot.autoconfigure.jdbc;

import java.sql.SQLException;

import oracle.ucp.jdbc.PoolDataSourceImpl;
import oracle.ucp.util.OpaqueString;
import org.junit.jupiter.api.Test;

import org.springframework.boot.jdbc.DatabaseDriver;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link OracleUcpJdbcConnectionDetailsBeanPostProcessor}.
 *
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 * @author Phillip Webb
 */
class OracleUcpJdbcConnectionDetailsBeanPostProcessorTests {

	@Test
	void setUsernamePasswordUrlAndDriverClassName() throws SQLException {
		PoolDataSourceImpl dataSource = new PoolDataSourceImpl();
		dataSource.setURL("will-be-overwritten");
		dataSource.setUser("will-be-overwritten");
		dataSource.setPassword("will-be-overwritten");
		dataSource.setConnectionFactoryClassName("will-be-overwritten");
		new OracleUcpJdbcConnectionDetailsBeanPostProcessor(null).processDataSource(dataSource,
				new TestJdbcConnectionDetails());
		assertThat(dataSource.getURL()).isEqualTo("jdbc:customdb://customdb.example.com:12345/database-1");
		assertThat(dataSource.getUser()).isEqualTo("user-1");
		assertThat(dataSource).extracting("password")
			.extracting((password) -> ((OpaqueString) password).get())
			.isEqualTo("password-1");
		assertThat(dataSource.getConnectionFactoryClassName())
			.isEqualTo(DatabaseDriver.POSTGRESQL.getDriverClassName());
	}

}
