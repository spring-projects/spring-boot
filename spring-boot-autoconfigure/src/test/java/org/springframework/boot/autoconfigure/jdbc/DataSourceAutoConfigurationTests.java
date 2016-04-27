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

package org.springframework.boot.autoconfigure.jdbc;

import java.net.URL;
import java.net.URLClassLoader;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverPropertyInfo;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.Properties;
import java.util.Random;
import java.util.logging.Logger;

import javax.sql.DataSource;

import com.zaxxer.hikari.HikariDataSource;
import org.apache.commons.dbcp.BasicDataSource;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import org.springframework.beans.factory.BeanCreationException;
import org.springframework.boot.autoconfigure.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.jdbc.DatabaseDriver;
import org.springframework.boot.test.util.EnvironmentTestUtils;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcOperations;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link DataSourceAutoConfiguration}.
 *
 * @author Dave Syer
 * @author Stephane Nicoll
 */
public class DataSourceAutoConfigurationTests {

	private final AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();

	@Before
	public void init() {
		EmbeddedDatabaseConnection.override = null;
		EnvironmentTestUtils.addEnvironment(this.context,
				"spring.datasource.initialize:false",
				"spring.datasource.url:jdbc:hsqldb:mem:testdb-" + new Random().nextInt());
	}

	@After
	public void restore() {
		EmbeddedDatabaseConnection.override = null;
		this.context.close();
	}

	@Test
	public void testDefaultDataSourceExists() throws Exception {
		this.context.register(DataSourceAutoConfiguration.class,
				PropertyPlaceholderAutoConfiguration.class);
		this.context.refresh();
		assertThat(this.context.getBean(DataSource.class)).isNotNull();
	}

	@Test
	public void testDataSourceHasEmbeddedDefault() throws Exception {
		this.context.register(DataSourceAutoConfiguration.class,
				PropertyPlaceholderAutoConfiguration.class);
		this.context.refresh();
		org.apache.tomcat.jdbc.pool.DataSource dataSource = this.context
				.getBean(org.apache.tomcat.jdbc.pool.DataSource.class);
		assertThat(dataSource.getUrl()).isNotNull();
		assertThat(dataSource.getDriverClassName()).isNotNull();
	}

	@Test(expected = BeanCreationException.class)
	public void testBadUrl() throws Exception {
		EnvironmentTestUtils.addEnvironment(this.context,
				"spring.datasource.url:jdbc:not-going-to-work");
		EmbeddedDatabaseConnection.override = EmbeddedDatabaseConnection.NONE;
		this.context.register(DataSourceAutoConfiguration.class,
				PropertyPlaceholderAutoConfiguration.class);
		this.context.refresh();
		assertThat(this.context.getBean(DataSource.class)).isNotNull();
	}

	@Test(expected = BeanCreationException.class)
	public void testBadDriverClass() throws Exception {
		EnvironmentTestUtils.addEnvironment(this.context,
				"spring.datasource.driverClassName:org.none.jdbcDriver",
				"spring.datasource.url:jdbc:hsqldb:mem:testdb");
		EmbeddedDatabaseConnection.override = EmbeddedDatabaseConnection.NONE;
		this.context.register(DataSourceAutoConfiguration.class,
				PropertyPlaceholderAutoConfiguration.class);
		this.context.refresh();
		assertThat(this.context.getBean(DataSource.class)).isNotNull();
	}

	@Test
	public void tomcatValidatesConnectionByDefault() {
		org.apache.tomcat.jdbc.pool.DataSource dataSource = autoConfigureDataSource(
				org.apache.tomcat.jdbc.pool.DataSource.class);
		assertThat(dataSource.isTestOnBorrow()).isTrue();
		assertThat(dataSource.getValidationQuery())
				.isEqualTo(DatabaseDriver.HSQLDB.getValidationQuery());
	}

	@Test
	public void hikariIsFallback() throws Exception {
		HikariDataSource dataSource = autoConfigureDataSource(HikariDataSource.class,
				"org.apache.tomcat");
		assertThat(dataSource.getJdbcUrl()).isEqualTo("jdbc:hsqldb:mem:testdb");
	}

	@Test
	public void hikariValidatesConnectionByDefault() throws Exception {
		HikariDataSource dataSource = autoConfigureDataSource(HikariDataSource.class,
				"org.apache.tomcat");
		assertThat(dataSource.getConnectionTestQuery()).isNull();
		// Use Connection#isValid()
	}

	@Test
	public void commonsDbcpIsFallback() throws Exception {
		BasicDataSource dataSource = autoConfigureDataSource(BasicDataSource.class,
				"org.apache.tomcat", "com.zaxxer.hikari");
		assertThat(dataSource.getUrl()).isEqualTo("jdbc:hsqldb:mem:testdb");
	}

	@Test
	public void commonsDbcpValidatesConnectionByDefault() {
		BasicDataSource dataSource = autoConfigureDataSource(BasicDataSource.class,
				"org.apache.tomcat", "com.zaxxer.hikari");
		assertThat(dataSource.getTestOnBorrow()).isTrue();
		assertThat(dataSource.getValidationQuery())
				.isEqualTo(DatabaseDriver.HSQLDB.getValidationQuery());
	}

	@Test
	public void commonsDbcp2IsFallback() throws Exception {
		org.apache.commons.dbcp2.BasicDataSource dataSource = autoConfigureDataSource(
				org.apache.commons.dbcp2.BasicDataSource.class, "org.apache.tomcat",
				"com.zaxxer.hikari", "org.apache.commons.dbcp.");
		assertThat(dataSource.getUrl()).isEqualTo("jdbc:hsqldb:mem:testdb");
	}

	@Test
	public void commonsDbcp2ValidatesConnectionByDefault() throws Exception {
		org.apache.commons.dbcp2.BasicDataSource dataSource = autoConfigureDataSource(
				org.apache.commons.dbcp2.BasicDataSource.class, "org.apache.tomcat",
				"com.zaxxer.hikari", "org.apache.commons.dbcp.");
		assertThat(dataSource.getTestOnBorrow()).isEqualTo(true);
		assertThat(dataSource.getValidationQuery()).isNull(); // Use Connection#isValid()
	}

	@Test
	public void testEmbeddedTypeDefaultsUsername() throws Exception {
		EnvironmentTestUtils.addEnvironment(this.context,
				"spring.datasource.driverClassName:org.hsqldb.jdbcDriver",
				"spring.datasource.url:jdbc:hsqldb:mem:testdb");
		this.context.register(DataSourceAutoConfiguration.class,
				PropertyPlaceholderAutoConfiguration.class);
		this.context.refresh();
		DataSource bean = this.context.getBean(DataSource.class);
		assertThat(bean).isNotNull();
		org.apache.tomcat.jdbc.pool.DataSource pool = (org.apache.tomcat.jdbc.pool.DataSource) bean;
		assertThat(pool.getDriverClassName()).isEqualTo("org.hsqldb.jdbcDriver");
		assertThat(pool.getUsername()).isEqualTo("sa");
	}

	@Test
	public void explicitType() {
		EnvironmentTestUtils.addEnvironment(this.context,
				"spring.datasource.driverClassName:org.hsqldb.jdbcDriver",
				"spring.datasource.url:jdbc:hsqldb:mem:testdb",
				"spring.datasource.type:" + HikariDataSource.class.getName());
		this.context.register(DataSourceAutoConfiguration.class,
				PropertyPlaceholderAutoConfiguration.class);
		this.context.refresh();
		DataSource bean = this.context.getBean(DataSource.class);
		assertThat(bean).isNotNull();
		assertThat(bean.getClass()).isEqualTo(HikariDataSource.class);
	}

	@Test
	public void testExplicitDriverClassClearsUsername() throws Exception {
		EnvironmentTestUtils.addEnvironment(this.context,
				"spring.datasource.driverClassName:"
						+ "org.springframework.boot.autoconfigure.jdbc."
						+ "DataSourceAutoConfigurationTests$DatabaseTestDriver",
				"spring.datasource.url:jdbc:foo://localhost");
		this.context.register(DataSourceAutoConfiguration.class,
				PropertyPlaceholderAutoConfiguration.class);
		this.context.refresh();
		DataSource bean = this.context.getBean(DataSource.class);
		assertThat(bean).isNotNull();
		org.apache.tomcat.jdbc.pool.DataSource pool = (org.apache.tomcat.jdbc.pool.DataSource) bean;
		assertThat(pool.getDriverClassName()).isEqualTo(
				"org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfigurationTests$DatabaseTestDriver");
		assertThat(pool.getUsername()).isNull();
	}

	@Test
	public void testDefaultDataSourceCanBeOverridden() throws Exception {
		this.context.register(TestDataSourceConfiguration.class,
				DataSourceAutoConfiguration.class,
				PropertyPlaceholderAutoConfiguration.class);
		this.context.refresh();
		DataSource dataSource = this.context.getBean(DataSource.class);
		assertThat(dataSource).isInstanceOf(BasicDataSource.class);
	}

	@Test
	public void testJdbcTemplateExists() throws Exception {
		this.context.register(DataSourceAutoConfiguration.class,
				PropertyPlaceholderAutoConfiguration.class);
		this.context.refresh();
		JdbcTemplate jdbcTemplate = this.context.getBean(JdbcTemplate.class);
		assertThat(jdbcTemplate).isNotNull();
		assertThat(jdbcTemplate.getDataSource()).isNotNull();
	}

	@Test
	public void testJdbcTemplateExistsWithCustomDataSource() throws Exception {
		this.context.register(TestDataSourceConfiguration.class,
				DataSourceAutoConfiguration.class,
				PropertyPlaceholderAutoConfiguration.class);
		this.context.refresh();
		JdbcTemplate jdbcTemplate = this.context.getBean(JdbcTemplate.class);
		assertThat(jdbcTemplate).isNotNull();
		assertThat(jdbcTemplate.getDataSource() instanceof BasicDataSource).isTrue();
	}

	@Test
	public void testNamedParameterJdbcTemplateExists() throws Exception {
		this.context.register(DataSourceAutoConfiguration.class,
				PropertyPlaceholderAutoConfiguration.class);
		this.context.refresh();
		assertThat(this.context.getBean(NamedParameterJdbcOperations.class)).isNotNull();
	}

	@SuppressWarnings("unchecked")
	private <T extends DataSource> T autoConfigureDataSource(Class<T> expectedType,
			final String... hiddenPackages) {
		EnvironmentTestUtils.addEnvironment(this.context,
				"spring.datasource.driverClassName:org.hsqldb.jdbcDriver",
				"spring.datasource.url:jdbc:hsqldb:mem:testdb");
		this.context.setClassLoader(
				new URLClassLoader(new URL[0], getClass().getClassLoader()) {

					@Override
					protected Class<?> loadClass(String name, boolean resolve)
							throws ClassNotFoundException {
						for (String hiddenPackage : hiddenPackages) {
							if (name.startsWith(hiddenPackage)) {
								throw new ClassNotFoundException();
							}
						}
						return super.loadClass(name, resolve);
					}

				});
		this.context.register(DataSourceAutoConfiguration.class,
				PropertyPlaceholderAutoConfiguration.class);
		this.context.refresh();
		DataSource bean = this.context.getBean(DataSource.class);
		assertThat(bean).isInstanceOf(expectedType);
		return (T) bean;
	}

	@Configuration
	static class TestDataSourceConfiguration {

		private BasicDataSource pool;

		@Bean
		public DataSource dataSource() {
			this.pool = new BasicDataSource();
			this.pool.setDriverClassName("org.hsqldb.jdbcDriver");
			this.pool.setUrl("jdbc:hsqldb:target/overridedb");
			this.pool.setUsername("sa");
			return this.pool;
		}

	}

	// see testExplicitDriverClassClearsUsername
	public static class DatabaseTestDriver implements Driver {

		@Override
		public Connection connect(String url, Properties info) throws SQLException {
			return mock(Connection.class);
		}

		@Override
		public boolean acceptsURL(String url) throws SQLException {
			return true;
		}

		@Override
		public DriverPropertyInfo[] getPropertyInfo(String url, Properties info)
				throws SQLException {
			return new DriverPropertyInfo[0];
		}

		@Override
		public int getMajorVersion() {
			return 1;
		}

		@Override
		public int getMinorVersion() {
			return 0;
		}

		@Override
		public boolean jdbcCompliant() {
			return false;
		}

		@Override
		public Logger getParentLogger() throws SQLFeatureNotSupportedException {
			return mock(Logger.class);
		}

	}

}
