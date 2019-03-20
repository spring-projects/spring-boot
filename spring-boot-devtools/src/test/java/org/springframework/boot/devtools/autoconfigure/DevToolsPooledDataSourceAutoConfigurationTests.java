/*
 * Copyright 2012-2017 the original author or authors.
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

package org.springframework.boot.devtools.autoconfigure;

import java.sql.SQLException;
import java.sql.Statement;

import javax.sql.DataSource;

import org.junit.Test;

import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.context.ConfigurableApplicationContext;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Tests for {@link DevToolsDataSourceAutoConfiguration} with a pooled data source.
 *
 * @author Andy Wilkinson
 */
public class DevToolsPooledDataSourceAutoConfigurationTests
		extends AbstractDevToolsDataSourceAutoConfigurationTests {

	@Test
	public void autoConfiguredInMemoryDataSourceIsShutdown() throws SQLException {
		ConfigurableApplicationContext context = createContext(
				DataSourceAutoConfiguration.class, DataSourceSpyConfiguration.class);
		Statement statement = configureDataSourceBehaviour(
				context.getBean(DataSource.class));
		context.close();
		verify(statement).execute("SHUTDOWN");
	}

	@Test
	public void autoConfiguredExternalDataSourceIsNotShutdown() throws SQLException {
		ConfigurableApplicationContext context = createContext("org.postgresql.Driver",
				DataSourceAutoConfiguration.class, DataSourceSpyConfiguration.class);
		Statement statement = configureDataSourceBehaviour(
				context.getBean(DataSource.class));
		context.close();
		verify(statement, times(0)).execute("SHUTDOWN");
	}

	@Test
	public void h2ServerIsNotShutdown() throws SQLException {
		ConfigurableApplicationContext context = createContext("org.h2.Driver",
				"jdbc:h2:hsql://localhost", DataSourceAutoConfiguration.class,
				DataSourceSpyConfiguration.class);
		Statement statement = configureDataSourceBehaviour(
				context.getBean(DataSource.class));
		context.close();
		verify(statement, times(0)).execute("SHUTDOWN");
	}

	@Test
	public void inMemoryH2IsShutdown() throws SQLException {
		ConfigurableApplicationContext context = createContext("org.h2.Driver",
				"jdbc:h2:mem:test", DataSourceAutoConfiguration.class,
				DataSourceSpyConfiguration.class);
		Statement statement = configureDataSourceBehaviour(
				context.getBean(DataSource.class));
		context.close();
		verify(statement, times(1)).execute("SHUTDOWN");
	}

	@Test
	public void hsqlServerIsNotShutdown() throws SQLException {
		ConfigurableApplicationContext context = createContext("org.hsqldb.jdbcDriver",
				"jdbc:hsqldb:hsql://localhost", DataSourceAutoConfiguration.class,
				DataSourceSpyConfiguration.class);
		Statement statement = configureDataSourceBehaviour(
				context.getBean(DataSource.class));
		context.close();
		verify(statement, times(0)).execute("SHUTDOWN");
	}

	@Test
	public void inMemoryHsqlIsShutdown() throws SQLException {
		ConfigurableApplicationContext context = createContext("org.hsqldb.jdbcDriver",
				"jdbc:hsqldb:mem:test", DataSourceAutoConfiguration.class,
				DataSourceSpyConfiguration.class);
		Statement statement = configureDataSourceBehaviour(
				context.getBean(DataSource.class));
		context.close();
		verify(statement, times(1)).execute("SHUTDOWN");
	}

	@Test
	public void derbyClientIsNotShutdown() throws SQLException {
		ConfigurableApplicationContext context = createContext(
				"org.apache.derby.jdbc.ClientDriver", "jdbc:derby://localhost",
				DataSourceAutoConfiguration.class, DataSourceSpyConfiguration.class);
		Statement statement = configureDataSourceBehaviour(
				context.getBean(DataSource.class));
		context.close();
		verify(statement, times(0)).execute("SHUTDOWN");
	}

	@Test
	public void inMemoryDerbyIsShutdown() throws SQLException {
		ConfigurableApplicationContext context = createContext(
				"org.apache.derby.jdbc.EmbeddedDriver", "jdbc:derby:memory:test",
				DataSourceAutoConfiguration.class, DataSourceSpyConfiguration.class);
		Statement statement = configureDataSourceBehaviour(
				context.getBean(DataSource.class));
		context.close();
		verify(statement, times(1)).execute("SHUTDOWN");
	}

}
