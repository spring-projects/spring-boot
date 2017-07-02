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
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.Random;
import java.util.function.Consumer;
import java.util.logging.Logger;

import javax.sql.DataSource;

import com.zaxxer.hikari.HikariDataSource;
import org.apache.commons.dbcp2.BasicDataSource;
import org.junit.Test;

import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.DatabaseDriver;
import org.springframework.boot.test.context.ContextConsumer;
import org.springframework.boot.test.context.ContextLoader;
import org.springframework.boot.test.context.HidePackagesClassLoader;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
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

	private final ContextLoader<AnnotationConfigApplicationContext> contextLoader = ContextLoader
			.standard().autoConfig(DataSourceAutoConfiguration.class)
			.env("spring.datasource.initialize=false",
					"spring.datasource.url:jdbc:hsqldb:mem:testdb-"
							+ new Random().nextInt());

	@Test
	public void testDefaultDataSourceExists() throws Exception {
		this.contextLoader.load(
				context -> assertThat(context.getBean(DataSource.class)).isNotNull());
	}

	@Test
	public void testDataSourceHasEmbeddedDefault() throws Exception {
		this.contextLoader.load(context -> {
			HikariDataSource dataSource = context.getBean(HikariDataSource.class);
			assertThat(dataSource.getJdbcUrl()).isNotNull();
			assertThat(dataSource.getDriverClassName()).isNotNull();
		});
	}

	@Test
	public void testBadUrl() throws Exception {
		try {
			EmbeddedDatabaseConnection.override = EmbeddedDatabaseConnection.NONE;
			this.contextLoader.env("spring.datasource.url:jdbc:not-going-to-work")
					.loadAndFail(BeanCreationException.class, ex -> {
					});
		}
		finally {
			EmbeddedDatabaseConnection.override = null;
		}
	}

	@Test
	public void testBadDriverClass() throws Exception {
		this.contextLoader.env("spring.datasource.driverClassName:org.none.jdbcDriver")
				.loadAndFail(BeanCreationException.class,
						ex -> assertThat(ex.getMessage())
								.contains("org.none.jdbcDriver"));
	}

	@Test
	public void hikariValidatesConnectionByDefault() throws Exception {
		assertDataSource(HikariDataSource.class,
				Collections.singletonList("org.apache.tomcat"), dataSource ->
				// Use Connection#isValid()
				assertThat(dataSource.getConnectionTestQuery()).isNull());
	}

	@Test
	public void tomcatIsFallback() throws Exception {
		assertDataSource(org.apache.tomcat.jdbc.pool.DataSource.class,
				Collections.singletonList("com.zaxxer.hikari"),
				dataSource -> assertThat(dataSource.getUrl())
						.startsWith("jdbc:hsqldb:mem:testdb"));
	}

	@Test
	public void tomcatValidatesConnectionByDefault() {
		assertDataSource(org.apache.tomcat.jdbc.pool.DataSource.class,
				Collections.singletonList("com.zaxxer.hikari"), dataSource -> {
					assertThat(dataSource.isTestOnBorrow()).isTrue();
					assertThat(dataSource.getValidationQuery())
							.isEqualTo(DatabaseDriver.HSQLDB.getValidationQuery());
				});
	}

	@Test
	public void commonsDbcp2IsFallback() throws Exception {
		assertDataSource(BasicDataSource.class,
				Arrays.asList("com.zaxxer.hikari", "org.apache.tomcat"),
				dataSource -> assertThat(dataSource.getUrl())
						.startsWith("jdbc:hsqldb:mem:testdb"));
	}

	@Test
	public void commonsDbcp2ValidatesConnectionByDefault() throws Exception {
		assertDataSource(org.apache.commons.dbcp2.BasicDataSource.class,
				Arrays.asList("com.zaxxer.hikari", "org.apache.tomcat"), dataSource -> {
					assertThat(dataSource.getTestOnBorrow()).isEqualTo(true);
					assertThat(dataSource.getValidationQuery()).isNull(); // Use
																			// Connection#isValid()
				});
	}

	@Test
	public void testEmbeddedTypeDefaultsUsername() throws Exception {
		this.contextLoader.env("spring.datasource.driverClassName:org.hsqldb.jdbcDriver",
				"spring.datasource.url:jdbc:hsqldb:mem:testdb").load(context -> {
					DataSource bean = context.getBean(DataSource.class);
					assertThat(bean).isNotNull();
					@SuppressWarnings("resource")
					HikariDataSource pool = (HikariDataSource) bean;
					assertThat(pool.getDriverClassName())
							.isEqualTo("org.hsqldb.jdbcDriver");
					assertThat(pool.getUsername()).isEqualTo("sa");
				});
	}

	/**
	 * This test makes sure that if no supported data source is present, a datasource is
	 * still created if "spring.datasource.type" is present.
	 */
	@Test
	public void explicitTypeNoSupportedDataSource() {
		this.contextLoader
				.classLoader(new HidePackagesClassLoader("org.apache.tomcat",
						"com.zaxxer.hikari", "org.apache.commons.dbcp",
						"org.apache.commons.dbcp2"))
				.env("spring.datasource.driverClassName:org.hsqldb.jdbcDriver",
						"spring.datasource.url:jdbc:hsqldb:mem:testdb",
						"spring.datasource.type:"
								+ SimpleDriverDataSource.class.getName())
				.load(testExplicitType());
	}

	@Test
	public void explicitTypeSupportedDataSource() {
		this.contextLoader
				.env("spring.datasource.driverClassName:org.hsqldb.jdbcDriver",
						"spring.datasource.url:jdbc:hsqldb:mem:testdb",
						"spring.datasource.type:"
								+ SimpleDriverDataSource.class.getName())
				.load(testExplicitType());
	}

	private ContextConsumer<AnnotationConfigApplicationContext> testExplicitType() {
		return context -> {
			assertThat(context.getBeansOfType(DataSource.class)).hasSize(1);
			DataSource bean = context.getBean(DataSource.class);
			assertThat(bean).isNotNull();
			assertThat(bean.getClass()).isEqualTo(SimpleDriverDataSource.class);
		};
	}

	@Test
	public void testExplicitDriverClassClearsUsername() throws Exception {
		this.contextLoader.env(
				"spring.datasource.driverClassName:" + DatabaseTestDriver.class.getName(),
				"spring.datasource.url:jdbc:foo://localhost").load(context -> {
					DataSource dataSource = context.getBean(DataSource.class);
					assertThat(dataSource).isNotNull();
					assertThat(((HikariDataSource) dataSource).getDriverClassName())
							.isEqualTo(DatabaseTestDriver.class.getName());
					assertThat(((HikariDataSource) dataSource).getUsername()).isNull();
				});
	}

	@Test
	public void testDefaultDataSourceCanBeOverridden() throws Exception {
		this.contextLoader.config(TestDataSourceConfiguration.class).load(context -> {
			DataSource dataSource = context.getBean(DataSource.class);
			assertThat(dataSource).isInstanceOf(BasicDataSource.class);
		});
	}

	@Test
	public void testDataSourceIsInitializedEarly() {
		this.contextLoader.config(TestInitializedDataSourceConfiguration.class)
				.env("spring.datasource.initialize=true")
				.load(context -> assertThat(context
						.getBean(TestInitializedDataSourceConfiguration.class).called)
								.isTrue());
	}

	private <T extends DataSource> void assertDataSource(Class<T> expectedType,
			List<String> hiddenPackages, Consumer<T> consumer) {
		HidePackagesClassLoader classLoader = new HidePackagesClassLoader(
				hiddenPackages.toArray(new String[hiddenPackages.size()]));
		this.contextLoader.classLoader(classLoader).load(context -> {
			DataSource bean = context.getBean(DataSource.class);
			assertThat(bean).isInstanceOf(expectedType);
			consumer.accept(expectedType.cast(bean));
		});
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

	@Configuration
	static class TestInitializedDataSourceConfiguration {

		private boolean called;

		@Autowired
		public void validateDataSourceIsInitialized(DataSource dataSource) {
			// Inject the datasource to validate it is initialized at the injection point
			JdbcTemplate template = new JdbcTemplate(dataSource);
			assertThat(template.queryForObject("SELECT COUNT(*) from BAR", Integer.class))
					.isEqualTo(1);
			this.called = true;
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
