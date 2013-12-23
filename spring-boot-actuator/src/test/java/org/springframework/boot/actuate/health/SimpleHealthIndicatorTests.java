/*
 * Copyright 2012-2013 the original author or authors.
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

package org.springframework.boot.actuate.health;

import java.sql.Connection;
import java.util.Map;

import javax.sql.DataSource;

import org.junit.Before;
import org.junit.Test;
import org.springframework.boot.autoconfigure.jdbc.EmbeddedDatabaseConnection;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;

import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author Dave Syer
 */
public class SimpleHealthIndicatorTests {

	private SimpleHealthIndicator indicator = new SimpleHealthIndicator();
	private DriverManagerDataSource dataSource;

	@Before
	public void init() {
		EmbeddedDatabaseConnection db = EmbeddedDatabaseConnection.HSQL;
		this.dataSource = new SingleConnectionDataSource(db.getUrl(), "sa", "", false);
		this.dataSource.setDriverClassName(db.getDriverClassName());
	}

	@Test
	public void database() {
		this.indicator.setDataSource(this.dataSource);
		Map<String, Object> health = this.indicator.health();
		assertNotNull(health.get("database"));
		assertNotNull(health.get("hello"));
	}

	@Test
	public void connectionClosed() throws Exception {
		DataSource dataSource = mock(DataSource.class);
		Connection connection = mock(Connection.class);
		when(connection.getMetaData()).thenReturn(
				this.dataSource.getConnection().getMetaData());
		when(dataSource.getConnection()).thenReturn(connection);
		this.indicator.setDataSource(dataSource);
		Map<String, Object> health = this.indicator.health();
		assertNotNull(health.get("database"));
		verify(connection, times(2)).close();
	}

}
