/*
 * Copyright 2012-2019 the original author or authors.
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
import java.util.Properties;

import javax.sql.DataSource;

import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.HikariPoolMXBean;
import org.apache.derby.jdbc.EmbeddedDriver;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Tests for {@link DevToolsDataSourceAutoConfiguration} with a pooled data source.
 *
 * @author Andy Wilkinson
 */
public class DevToolsPooledDataSourceAutoConfigurationTests extends AbstractDevToolsDataSourceAutoConfigurationTests {

	@Before
	public void before() {
		System.setProperty("derby.stream.error.file", "target/derby.log");
	}

	@After
	public void after() {
		System.clearProperty("derby.stream.error.file");
	}

	@Test
	public void autoConfiguredInMemoryDataSourceIsShutdown() throws SQLException {
		ConfigurableApplicationContext context = createContext(DataSourceAutoConfiguration.class,
				DataSourceSpyConfiguration.class);
		Statement statement = configureDataSourceBehavior(context.getBean(DataSource.class));
		context.close();
		verify(statement).execute("SHUTDOWN");
	}

	@Test
	public void autoConfiguredExternalDataSourceIsNotShutdown() throws SQLException {
		ConfigurableApplicationContext context = createContext("org.postgresql.Driver",
				DataSourceAutoConfiguration.class, DataSourceSpyConfiguration.class);
		Statement statement = configureDataSourceBehavior(context.getBean(DataSource.class));
		context.close();
		verify(statement, never()).execute("SHUTDOWN");
	}

	@Test
	public void h2ServerIsNotShutdown() throws SQLException {
		ConfigurableApplicationContext context = createContext("org.h2.Driver", "jdbc:h2:hsql://localhost",
				DataSourceAutoConfiguration.class, DataSourceSpyConfiguration.class);
		Statement statement = configureDataSourceBehavior(context.getBean(DataSource.class));
		context.close();
		verify(statement, never()).execute("SHUTDOWN");
	}

	@Test
	public void inMemoryH2IsShutdown() throws SQLException {
		ConfigurableApplicationContext context = createContext("org.h2.Driver", "jdbc:h2:mem:test",
				DataSourceAutoConfiguration.class, DataSourceSpyConfiguration.class);
		Statement statement = configureDataSourceBehavior(context.getBean(DataSource.class));
		context.close();
		verify(statement, times(1)).execute("SHUTDOWN");
	}

	@Test
	public void hsqlServerIsNotShutdown() throws SQLException {
		ConfigurableApplicationContext context = createContext("org.hsqldb.jdbcDriver", "jdbc:hsqldb:hsql://localhost",
				DataSourceAutoConfiguration.class, DataSourceSpyConfiguration.class);
		Statement statement = configureDataSourceBehavior(context.getBean(DataSource.class));
		context.close();
		verify(statement, never()).execute("SHUTDOWN");
	}

	@Test
	public void inMemoryHsqlIsShutdown() throws SQLException {
		ConfigurableApplicationContext context = createContext("org.hsqldb.jdbcDriver", "jdbc:hsqldb:mem:test",
				DataSourceAutoConfiguration.class, DataSourceSpyConfiguration.class);
		Statement statement = configureDataSourceBehavior(context.getBean(DataSource.class));
		context.close();
		verify(statement, times(1)).execute("SHUTDOWN");
	}

	@Test
	public void derbyClientIsNotShutdown() throws SQLException {
		ConfigurableApplicationContext context = createContext("org.apache.derby.jdbc.ClientDriver",
				"jdbc:derby://localhost", DataSourceAutoConfiguration.class, DataSourceSpyConfiguration.class);
		Statement statement = configureDataSourceBehavior(context.getBean(DataSource.class));
		context.close();
		verify(statement, never()).execute("SHUTDOWN");
	}

	@Test
	public void inMemoryDerbyIsShutdown() throws SQLException {
		ConfigurableApplicationContext context = createContext("org.apache.derby.jdbc.EmbeddedDriver",
				"jdbc:derby:memory:test;create=true", DataSourceAutoConfiguration.class,
				DataSourceSpyConfiguration.class);
		HikariDataSource dataSource = context.getBean(HikariDataSource.class);
		JdbcTemplate jdbc = new JdbcTemplate(dataSource);
		jdbc.execute("SELECT 1 FROM SYSIBM.SYSDUMMY1");
		HikariPoolMXBean pool = dataSource.getHikariPoolMXBean();
		// Prevent a race between Hikari's initialization and Derby shutdown
		long end = System.currentTimeMillis() + 30000;
		while (pool.getIdleConnections() != dataSource.getMinimumIdle()) {
			if (System.currentTimeMillis() >= end) {
				throw new IllegalStateException("DataSource did not become idle within 30 seconds");
			}
			try {
				Thread.sleep(100);
			}
			catch (InterruptedException ex) {
				Thread.currentThread().interrupt();
			}
		}
		context.close();
		// Connect should fail as DB no longer exists
		assertThatExceptionOfType(SQLException.class)
				.isThrownBy(() -> new EmbeddedDriver().connect("jdbc:derby:memory:test", new Properties()))
				.satisfies((ex) -> assertThat(ex.getSQLState()).isEqualTo("XJ004"));
	}

}
