/*
 * Copyright 2012-2016 the original author or authors.
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

package org.springframework.boot.devtools.autoconfigure;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import javax.sql.DataSource;

import org.junit.Test;

import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.EnvironmentTestUtils;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabase;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Tests for {@link DevToolsDataSourceAutoConfiguration}.
 *
 * @author Andy Wilkinson
 */
public class DevToolsDataSourceAutoConfigurationTests {

	@Test
	public void embeddedDatabaseIsNotShutDown() throws SQLException {
		ConfigurableApplicationContext context = createContext("org.h2.Driver",
				EmbeddedDatabaseConfiguration.class);
		DataSource dataSource = context.getBean(DataSource.class);
		context.close();
		verify(dataSource, times(0)).getConnection();
	}

	@Test
	public void externalDatabaseIsNotShutDown() throws SQLException {
		ConfigurableApplicationContext context = createContext("org.postgresql.Driver",
				DataSourceConfiguration.class);
		DataSource dataSource = context.getBean(DataSource.class);
		context.close();
		verify(dataSource, times(0)).getConnection();
	}

	@Test
	public void nonEmbeddedInMemoryDatabaseIsShutDown() throws SQLException {
		ConfigurableApplicationContext context = createContext("org.h2.Driver",
				DataSourceConfiguration.class);
		DataSource dataSource = context.getBean(DataSource.class);
		Connection connection = mock(Connection.class);
		given(dataSource.getConnection()).willReturn(connection);
		Statement statement = mock(Statement.class);
		given(connection.createStatement()).willReturn(statement);
		context.close();
		verify(statement).execute("SHUTDOWN");
	}

	private ConfigurableApplicationContext createContext(String driver,
			Class<?>... classes) {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
		context.register(classes);
		context.register(DevToolsDataSourceAutoConfiguration.class);
		EnvironmentTestUtils.addEnvironment(context,
				"spring.datasource.driver-class-name:" + driver);
		context.refresh();
		return context;
	}

	@Configuration
	@EnableConfigurationProperties(DataSourceProperties.class)
	static class EmbeddedDatabaseConfiguration {

		@Bean
		public EmbeddedDatabase embeddedDatabase() {
			return mock(EmbeddedDatabase.class);
		}
	}

	@Configuration
	@EnableConfigurationProperties(DataSourceProperties.class)
	static class DataSourceConfiguration {

		@Bean
		public DataSource in() {
			return mock(DataSource.class);
		}

	}

}
