/*
 * Copyright 2012-2017 the original author or authors.
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

package org.springframework.boot.autoconfigure.jooq;

import java.sql.Connection;
import java.sql.DatabaseMetaData;

import javax.sql.DataSource;

import org.jooq.SQLDialect;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link SQLDialectLookup}.
 *
 * @author Michael Simons
 */
public class SQLDialectLookupTests {

	@Test
	public void getDatabaseWhenDataSourceIsNullShouldReturnDefault() throws Exception {
		assertThat(SQLDialectLookup.getDialect(null)).isEqualTo(SQLDialect.DEFAULT);
	}

	@Test
	public void getDatabaseWhenDataSourceIsUnknownShouldReturnDefault() throws Exception {
		testGetDatabase("jdbc:idontexist:", SQLDialect.DEFAULT);
	}

	@Test
	public void getDatabaseWhenDerbyShouldReturnDerby() throws Exception {
		testGetDatabase("jdbc:derby:", SQLDialect.DERBY);
	}

	@Test
	public void getDatabaseWhenH2ShouldReturnH2() throws Exception {
		testGetDatabase("jdbc:h2:", SQLDialect.H2);
	}

	@Test
	public void getDatabaseWhenHsqldbShouldReturnHsqldb() throws Exception {
		testGetDatabase("jdbc:hsqldb:", SQLDialect.HSQLDB);
	}

	@Test
	public void getDatabaseWhenMysqlShouldReturnMysql() throws Exception {
		testGetDatabase("jdbc:mysql:", SQLDialect.MYSQL);
	}

	@Test
	public void getDatabaseWhenOracleShouldReturnOracle() throws Exception {
		testGetDatabase("jdbc:oracle:", SQLDialect.DEFAULT);
	}

	@Test
	public void getDatabaseWhenPostgresShouldReturnPostgres() throws Exception {
		testGetDatabase("jdbc:postgresql:", SQLDialect.POSTGRES);
	}

	@Test
	public void getDatabaseWhenSqlserverShouldReturnSqlserver() throws Exception {
		testGetDatabase("jdbc:sqlserver:", SQLDialect.DEFAULT);
	}

	@Test
	public void getDatabaseWhenDb2ShouldReturnDb2() throws Exception {
		testGetDatabase("jdbc:db2:", SQLDialect.DEFAULT);
	}

	@Test
	public void getDatabaseWhenInformixShouldReturnInformix() throws Exception {
		testGetDatabase("jdbc:informix-sqli:", SQLDialect.DEFAULT);
	}

	private void testGetDatabase(String url, SQLDialect expected) throws Exception {
		DataSource dataSource = mock(DataSource.class);
		Connection connection = mock(Connection.class);
		DatabaseMetaData metaData = mock(DatabaseMetaData.class);
		given(dataSource.getConnection()).willReturn(connection);
		given(connection.getMetaData()).willReturn(metaData);
		given(metaData.getURL()).willReturn(url);
		SQLDialect sQLDialect = SQLDialectLookup.getDialect(dataSource);
		assertThat(sQLDialect).isEqualTo(expected);
	}

}
