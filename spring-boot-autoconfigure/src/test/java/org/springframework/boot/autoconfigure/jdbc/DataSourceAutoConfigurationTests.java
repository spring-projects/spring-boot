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

package org.springframework.boot.autoconfigure.jdbc;

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
import org.apache.commons.dbcp2.BasicDataSource;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import org.springframework.beans.factory.BeanCreationException;
import org.springframework.boot.jdbc.DatabaseDriver;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.SimpleDriverDataSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link DataSourceAutoConfiguration}.
 *
 * @author Dave Syer
 * @author Stephane Nicoll
 */
public class DataSourceAutoConfigurationTests {

	@Rule
	public final ExpectedException thrown = ExpectedException.none();

	private ConfigurableApplicationContext context;

	@After
	public void close() {
		if (this.context != null) {
			this.context.close();
		}
	}

	@Test
	public void testDefaultDataSourceExists() throws Exception {
		load();
		assertThat(this.context.getBean(DataSource.class)).isNotNull();
	}

	@Test
	public void testDataSourceHasEmbeddedDefault() throws Exception {
		load();
		HikariDataSource dataSource = this.context.getBean(HikariDataSource.class);
		assertThat(dataSource.getJdbcUrl()).isNotNull();
		assertThat(dataSource.getDriverClassName()).isNotNull();
	}

	@Test
	public void testBadUrl() throws Exception {
		try {
			EmbeddedDatabaseConnection.override = EmbeddedDatabaseConnection.NONE;
			this.thrown.expect(BeanCreationException.class);
			load("spring.datasource.url:jdbc:not-going-to-work");
		}
		finally {
			EmbeddedDatabaseConnection.override = null;
		}
	}

	@Test
	public void testBadDriverClass() throws Exception {
		this.thrown.expect(BeanCreationException.class);
		this.thrown.expectMessage("org.none.jdbcDriver");
		load("spring.datasource.driverClassName:org.none.jdbcDriver");
	}

	@Test
	public void hikariValidatesConnectionByDefault() throws Exception {
		HikariDataSource dataSource = autoConfigureDataSource(HikariDataSource.class,
				"org.apache.tomcat");
		assertThat(dataSource.getConnectionTestQuery()).isNull();
		// Use Connection#isValid()
	}

	@Test
	public void tomcatIsFallback() throws Exception {
		org.apache.tomcat.jdbc.pool.DataSource dataSource = autoConfigureDataSource(
				org.apache.tomcat.jdbc.pool.DataSource.class, "com.zaxxer.hikari");
		assertThat(dataSource.getUrl()).startsWith("jdbc:hsqldb:mem:testdb");
	}

	@Test
	public void tomcatValidatesConnectionByDefault() {
		org.apache.tomcat.jdbc.pool.DataSource dataSource = autoConfigureDataSource(
				org.apache.tomcat.jdbc.pool.DataSource.class, "com.zaxxer.hikari");
		assertThat(dataSource.isTestOnBorrow()).isTrue();
		assertThat(dataSource.getValidationQuery())
				.isEqualTo(DatabaseDriver.HSQLDB.getValidationQuery());
	}

	@Test
	public void commonsDbcp2IsFallback() throws Exception {
		BasicDataSource dataSource = autoConfigureDataSource(BasicDataSource.class,
				"com.zaxxer.hikari", "org.apache.tomcat");
		assertThat(dataSource.getUrl()).startsWith("jdbc:hsqldb:mem:testdb");
	}

	@Test
	public void commonsDbcp2ValidatesConnectionByDefault() throws Exception {
		org.apache.commons.dbcp2.BasicDataSource dataSource = autoConfigureDataSource(
				org.apache.commons.dbcp2.BasicDataSource.class, "com.zaxxer.hikari",
				"org.apache.tomcat");
		assertThat(dataSource.getTestOnBorrow()).isEqualTo(true);
		assertThat(dataSource.getValidationQuery()).isNull(); // Use Connection#isValid()
	}

	@Test
	public void testEmbeddedTypeDefaultsUsername() throws Exception {
		load("spring.datasource.driverClassName:org.hsqldb.jdbcDriver",
				"spring.datasource.url:jdbc:hsqldb:mem:testdb");
		DataSource bean = this.context.getBean(DataSource.class);
		assertThat(bean).isNotNull();
		@SuppressWarnings("resource")
		HikariDataSource pool = (HikariDataSource) bean;
		assertThat(pool.getDriverClassName()).isEqualTo("org.hsqldb.jdbcDriver");
		assertThat(pool.getUsername()).isEqualTo("sa");
	}

	/**
	 * This test makes sure that if no supported data source is present, a datasource is
	 * still created if "spring.datasource.type" is present.
	 */
	@Test
	public void explicitTypeNoSupportedDataSource() {
		load(null, new HidePackagesClassLoader("org.apache.tomcat", "com.zaxxer.hikari",
						"org.apache.commons.dbcp", "org.apache.commons.dbcp2"),
				"spring.datasource.driverClassName:org.hsqldb.jdbcDriver",
				"spring.datasource.url:jdbc:hsqldb:mem:testdb",
				"spring.datasource.type:"
						+ SimpleDriverDataSource.class.getName());
		testExplicitType();
	}

	@Test
	public void explicitTypeSupportedDataSource() {
		load("spring.datasource.driverClassName:org.hsqldb.jdbcDriver",
				"spring.datasource.url:jdbc:hsqldb:mem:testdb",
				"spring.datasource.type:"
						+ SimpleDriverDataSource.class.getName());
		testExplicitType();
	}

	private void testExplicitType() {
		assertThat(this.context.getBeansOfType(DataSource.class)).hasSize(1);
		DataSource bean = this.context.getBean(DataSource.class);
		assertThat(bean).isNotNull();
		assertThat(bean.getClass()).isEqualTo(SimpleDriverDataSource.class);
	}

	@Test
	public void testExplicitDriverClassClearsUsername() throws Exception {
		load("spring.datasource.driverClassName:" + DatabaseTestDriver.class.getName(),
				"spring.datasource.url:jdbc:foo://localhost");
		DataSource dataSource = this.context.getBean(DataSource.class);
		assertThat(dataSource).isNotNull();
		assertThat(((HikariDataSource) dataSource).getDriverClassName())
				.isEqualTo(DatabaseTestDriver.class.getName());
		assertThat(((HikariDataSource) dataSource).getUsername()).isNull();
	}

	@Test
	public void testDefaultDataSourceCanBeOverridden() throws Exception {
		load(TestDataSourceConfiguration.class);
		DataSource dataSource = this.context.getBean(DataSource.class);
		assertThat(dataSource).isInstanceOf(BasicDataSource.class);
	}

	@SuppressWarnings("unchecked")
	private <T extends DataSource> T autoConfigureDataSource(Class<T> expectedType,
			final String... hiddenPackages) {
		load(null, new HidePackagesClassLoader(hiddenPackages));
		DataSource bean = this.context.getBean(DataSource.class);
		assertThat(bean).isInstanceOf(expectedType);
		return (T) bean;
	}

	public void load(String... environment) {
		load(null, environment);
	}

	public void load(Class<?> config, String... environment) {
		load(config, null, environment);
	}

	public void load(Class<?> config, ClassLoader classLoader, String... environment) {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
		if (classLoader != null) {
			ctx.setClassLoader(classLoader);
		}
		TestPropertyValues
				.of("spring.datasource.initialize=false",
						"spring.datasource.url:jdbc:hsqldb:mem:testdb-" + new Random().nextInt())
				.applyTo(ctx);
		TestPropertyValues.of(environment).applyTo(ctx);
		if (config != null) {
			ctx.register(config);
		}
		ctx.register(DataSourceAutoConfiguration.class);
		ctx.refresh();
		this.context = ctx;
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
