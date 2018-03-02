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

package org.springframework.boot.autoconfigure.orm.jpa;

import java.sql.Connection;
import java.sql.DatabaseMetaData;

import javax.sql.DataSource;

import org.junit.Test;

import org.springframework.orm.jpa.vendor.Database;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link DatabaseLookup}.
 *
 * @author Eddú Meléndez
 * @author Phillip Webb
 */
public class DatabaseLookupTests {

	@Test
	public void getDatabaseWhenDataSourceIsNullShouldReturnDefault() {
		assertThat(DatabaseLookup.getDatabase(null)).isEqualTo(Database.DEFAULT);
	}

	@Test
	public void getDatabaseWhenDataSourceIsUnknownShouldReturnDefault() throws Exception {
		testGetDatabase("jdbc:idontexist:", Database.DEFAULT);
	}

	@Test
	public void getDatabaseWhenDerbyShouldReturnDerby() throws Exception {
		testGetDatabase("jdbc:derby:", Database.DERBY);
	}

	@Test
	public void getDatabaseWhenH2ShouldReturnH2() throws Exception {
		testGetDatabase("jdbc:h2:", Database.H2);
	}

	@Test
	public void getDatabaseWhenHsqldbShouldReturnHsqldb() throws Exception {
		testGetDatabase("jdbc:hsqldb:", Database.HSQL);
	}

	@Test
	public void getDatabaseWhenMysqlShouldReturnMysql() throws Exception {
		testGetDatabase("jdbc:mysql:", Database.MYSQL);
	}

	@Test
	public void getDatabaseWhenOracleShouldReturnOracle() throws Exception {
		testGetDatabase("jdbc:oracle:", Database.ORACLE);
	}

	@Test
	public void getDatabaseWhenPostgresShouldReturnPostgres() throws Exception {
		testGetDatabase("jdbc:postgresql:", Database.POSTGRESQL);
	}

	@Test
	public void getDatabaseWhenSqlserverShouldReturnSqlserver() throws Exception {
		testGetDatabase("jdbc:sqlserver:", Database.SQL_SERVER);
	}

	@Test
	public void getDatabaseWhenDb2ShouldReturnDb2() throws Exception {
		testGetDatabase("jdbc:db2:", Database.DB2);
	}

	@Test
	public void getDatabaseWhenInformixShouldReturnInformix() throws Exception {
		testGetDatabase("jdbc:informix-sqli:", Database.INFORMIX);
	}

	private void testGetDatabase(String url, Database expected) throws Exception {
		DataSource dataSource = mock(DataSource.class);
		Connection connection = mock(Connection.class);
		DatabaseMetaData metaData = mock(DatabaseMetaData.class);
		given(dataSource.getConnection()).willReturn(connection);
		given(connection.getMetaData()).willReturn(metaData);
		given(metaData.getURL()).willReturn(url);
		Database database = DatabaseLookup.getDatabase(dataSource);
		assertThat(database).isEqualTo(expected);
	}

}
