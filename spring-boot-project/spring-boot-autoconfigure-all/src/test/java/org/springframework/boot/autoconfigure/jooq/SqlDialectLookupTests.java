/*
 * Copyright 2012-2022 the original author or authors.
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

package org.springframework.boot.autoconfigure.jooq;

import java.sql.Connection;
import java.sql.DatabaseMetaData;

import javax.sql.DataSource;

import org.jooq.SQLDialect;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link SqlDialectLookup}.
 *
 * @author Michael Simons
 * @author Stephane Nicoll
 */
class SqlDialectLookupTests {

	@Test
	void getSqlDialectWhenDataSourceIsNullShouldReturnDefault() {
		assertThat(SqlDialectLookup.getDialect(null)).isEqualTo(SQLDialect.DEFAULT);
	}

	@Test
	void getSqlDialectWhenDataSourceIsUnknownShouldReturnDefault() throws Exception {
		testGetSqlDialect("jdbc:idontexist:", SQLDialect.DEFAULT);
	}

	@Test
	void getSqlDialectWhenDerbyShouldReturnDerby() throws Exception {
		testGetSqlDialect("jdbc:derby:", SQLDialect.DERBY);
	}

	@Test
	void getSqlDialectWhenH2ShouldReturnH2() throws Exception {
		testGetSqlDialect("jdbc:h2:", SQLDialect.H2);
	}

	@Test
	void getSqlDialectWhenHsqldbShouldReturnHsqldb() throws Exception {
		testGetSqlDialect("jdbc:hsqldb:", SQLDialect.HSQLDB);
	}

	@Test
	void getSqlDialectWhenMysqlShouldReturnMysql() throws Exception {
		testGetSqlDialect("jdbc:mysql:", SQLDialect.MYSQL);
	}

	@Test
	void getSqlDialectWhenOracleShouldReturnDefault() throws Exception {
		testGetSqlDialect("jdbc:oracle:", SQLDialect.DEFAULT);
	}

	@Test
	void getSqlDialectWhenPostgresShouldReturnPostgres() throws Exception {
		testGetSqlDialect("jdbc:postgresql:", SQLDialect.POSTGRES);
	}

	@Test
	void getSqlDialectWhenSqlserverShouldReturnDefault() throws Exception {
		testGetSqlDialect("jdbc:sqlserver:", SQLDialect.DEFAULT);
	}

	@Test
	void getSqlDialectWhenDb2ShouldReturnDefault() throws Exception {
		testGetSqlDialect("jdbc:db2:", SQLDialect.DEFAULT);
	}

	@Test
	void getSqlDialectWhenInformixShouldReturnDefault() throws Exception {
		testGetSqlDialect("jdbc:informix-sqli:", SQLDialect.DEFAULT);
	}

	private void testGetSqlDialect(String url, SQLDialect expected) throws Exception {
		DataSource dataSource = mock(DataSource.class);
		Connection connection = mock(Connection.class);
		DatabaseMetaData metaData = mock(DatabaseMetaData.class);
		given(dataSource.getConnection()).willReturn(connection);
		given(connection.getMetaData()).willReturn(metaData);
		given(metaData.getURL()).willReturn(url);
		SQLDialect sqlDialect = SqlDialectLookup.getDialect(dataSource);
		assertThat(sqlDialect).isEqualTo(expected);
	}

}
