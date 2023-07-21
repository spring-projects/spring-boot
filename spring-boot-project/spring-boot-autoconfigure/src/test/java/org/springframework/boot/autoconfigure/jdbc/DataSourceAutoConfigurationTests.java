/*
 * Copyright 2012-2023 the original author or authors.
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

package org.springframework.boot.autoconfigure.jdbc;

import java.net.URL;
import java.net.URLClassLoader;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverPropertyInfo;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.Random;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.logging.Logger;

import javax.sql.DataSource;

import com.zaxxer.hikari.HikariDataSource;
import io.r2dbc.spi.ConnectionFactory;
import oracle.ucp.jdbc.PoolDataSourceImpl;
import org.apache.commons.dbcp2.BasicDataSource;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.BeanCreationException;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.jdbc.DatabaseDriver;
import org.springframework.boot.jdbc.EmbeddedDatabaseConnection;
import org.springframework.boot.jdbc.init.DataSourceScriptDatabaseInitializer;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.assertj.AssertableApplicationContext;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.SimpleDriverDataSource;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabase;
import org.springframework.util.StringUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link DataSourceAutoConfiguration}.
 *
 * @author Dave Syer
 * @author Stephane Nicoll
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 * @author Phillip Webb
 */
class DataSourceAutoConfigurationTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(DataSourceAutoConfiguration.class))
		.withPropertyValues("spring.datasource.url:jdbc:hsqldb:mem:testdb-" + new Random().nextInt());

	@Test
	void testDefaultDataSourceExists() {
		this.contextRunner.run((context) -> assertThat(context).hasSingleBean(DataSource.class));
	}

	@Test
	void testDataSourceHasEmbeddedDefault() {
		this.contextRunner.run((context) -> {
			HikariDataSource dataSource = context.getBean(HikariDataSource.class);
			assertThat(dataSource.getJdbcUrl()).isNotNull();
			assertThat(dataSource.getDriverClassName()).isNotNull();
		});
	}

	@Test
	void testBadUrl() {
		this.contextRunner.withPropertyValues("spring.datasource.url:jdbc:not-going-to-work")
			.withClassLoader(new DisableEmbeddedDatabaseClassLoader())
			.run((context) -> assertThat(context).getFailure().isInstanceOf(BeanCreationException.class));
	}

	@Test
	void testBadDriverClass() {
		this.contextRunner.withPropertyValues("spring.datasource.driverClassName:org.none.jdbcDriver")
			.run((context) -> assertThat(context).getFailure()
				.isInstanceOf(BeanCreationException.class)
				.hasMessageContaining("org.none.jdbcDriver"));
	}

	@Test
	void datasourceWhenConnectionFactoryPresentIsNotAutoConfigured() {
		this.contextRunner.withBean(ConnectionFactory.class, () -> mock(ConnectionFactory.class))
			.run((context) -> assertThat(context).doesNotHaveBean(DataSource.class));
	}

	@Test
	void hikariValidatesConnectionByDefault() {
		assertDataSource(HikariDataSource.class, Collections.singletonList("org.apache.tomcat"), (dataSource) ->
		// Use Connection#isValid()
		assertThat(dataSource.getConnectionTestQuery()).isNull());
	}

	@Test
	void tomcatIsFallback() {
		assertDataSource(org.apache.tomcat.jdbc.pool.DataSource.class, Collections.singletonList("com.zaxxer.hikari"),
				(dataSource) -> assertThat(dataSource.getUrl()).startsWith("jdbc:hsqldb:mem:testdb"));
	}

	@Test
	void tomcatValidatesConnectionByDefault() {
		assertDataSource(org.apache.tomcat.jdbc.pool.DataSource.class, Collections.singletonList("com.zaxxer.hikari"),
				(dataSource) -> {
					assertThat(dataSource.isTestOnBorrow()).isTrue();
					assertThat(dataSource.getValidationQuery()).isEqualTo(DatabaseDriver.HSQLDB.getValidationQuery());
				});
	}

	@Test
	void commonsDbcp2IsFallback() {
		assertDataSource(BasicDataSource.class, Arrays.asList("com.zaxxer.hikari", "org.apache.tomcat"),
				(dataSource) -> assertThat(dataSource.getUrl()).startsWith("jdbc:hsqldb:mem:testdb"));
	}

	@Test
	void commonsDbcp2ValidatesConnectionByDefault() {
		assertDataSource(org.apache.commons.dbcp2.BasicDataSource.class,
				Arrays.asList("com.zaxxer.hikari", "org.apache.tomcat"), (dataSource) -> {
					assertThat(dataSource.getTestOnBorrow()).isTrue();
					// Use Connection#isValid()
					assertThat(dataSource.getValidationQuery()).isNull();
				});
	}

	@Test
	void oracleUcpIsFallback() {
		assertDataSource(PoolDataSourceImpl.class,
				Arrays.asList("com.zaxxer.hikari", "org.apache.tomcat", "org.apache.commons.dbcp2"),
				(dataSource) -> assertThat(dataSource.getURL()).startsWith("jdbc:hsqldb:mem:testdb"));
	}

	@Test
	void oracleUcpValidatesConnectionByDefault() {
		assertDataSource(PoolDataSourceImpl.class,
				Arrays.asList("com.zaxxer.hikari", "org.apache.tomcat", "org.apache.commons.dbcp2"), (dataSource) -> {
					assertThat(dataSource.getValidateConnectionOnBorrow()).isTrue();
					// Use an internal ping when using an Oracle JDBC driver
					assertThat(dataSource.getSQLForValidateConnection()).isNull();
				});
	}

	@Test
	@SuppressWarnings("resource")
	void testEmbeddedTypeDefaultsUsername() {
		this.contextRunner
			.withPropertyValues("spring.datasource.driverClassName:org.hsqldb.jdbcDriver",
					"spring.datasource.url:jdbc:hsqldb:mem:testdb")
			.run((context) -> {
				DataSource bean = context.getBean(DataSource.class);
				HikariDataSource pool = (HikariDataSource) bean;
				assertThat(pool.getDriverClassName()).isEqualTo("org.hsqldb.jdbcDriver");
				assertThat(pool.getUsername()).isEqualTo("sa");
			});
	}

	@Test
	void dataSourceWhenNoConnectionPoolsAreAvailableWithUrlDoesNotCreateDataSource() {
		this.contextRunner.with(hideConnectionPools())
			.withPropertyValues("spring.datasource.url:jdbc:hsqldb:mem:testdb")
			.run((context) -> assertThat(context).doesNotHaveBean(DataSource.class));
	}

	/**
	 * This test makes sure that if no supported data source is present, a datasource is
	 * still created if "spring.datasource.type" is present.
	 */
	@Test
	void dataSourceWhenNoConnectionPoolsAreAvailableWithUrlAndTypeCreatesDataSource() {
		this.contextRunner.with(hideConnectionPools())
			.withPropertyValues("spring.datasource.driverClassName:org.hsqldb.jdbcDriver",
					"spring.datasource.url:jdbc:hsqldb:mem:testdb",
					"spring.datasource.type:" + SimpleDriverDataSource.class.getName())
			.run(this::containsOnlySimpleDriverDataSource);
	}

	@Test
	void explicitTypeSupportedDataSource() {
		this.contextRunner
			.withPropertyValues("spring.datasource.driverClassName:org.hsqldb.jdbcDriver",
					"spring.datasource.url:jdbc:hsqldb:mem:testdb",
					"spring.datasource.type:" + SimpleDriverDataSource.class.getName())
			.run(this::containsOnlySimpleDriverDataSource);
	}

	private void containsOnlySimpleDriverDataSource(AssertableApplicationContext context) {
		assertThat(context).hasSingleBean(DataSource.class);
		assertThat(context).getBean(DataSource.class).isExactlyInstanceOf(SimpleDriverDataSource.class);
	}

	@Test
	void testExplicitDriverClassClearsUsername() {
		this.contextRunner
			.withPropertyValues("spring.datasource.driverClassName:" + DatabaseTestDriver.class.getName(),
					"spring.datasource.url:jdbc:foo://localhost")
			.run((context) -> {
				assertThat(context).hasSingleBean(DataSource.class);
				HikariDataSource dataSource = context.getBean(HikariDataSource.class);
				assertThat(dataSource.getDriverClassName()).isEqualTo(DatabaseTestDriver.class.getName());
				assertThat(dataSource.getUsername()).isNull();
			});
	}

	@Test
	void testDefaultDataSourceCanBeOverridden() {
		this.contextRunner.withUserConfiguration(TestDataSourceConfiguration.class)
			.run((context) -> assertThat(context).getBean(DataSource.class).isInstanceOf(BasicDataSource.class));
	}

	@Test
	void whenThereIsAUserProvidedDataSourceAnUnresolvablePlaceholderDoesNotCauseAProblem() {
		this.contextRunner.withUserConfiguration(TestDataSourceConfiguration.class)
			.withPropertyValues("spring.datasource.url:${UNRESOLVABLE_PLACEHOLDER}")
			.run((context) -> assertThat(context).getBean(DataSource.class).isInstanceOf(BasicDataSource.class));
	}

	@Test
	void whenThereIsAnEmptyUserProvidedDataSource() {
		this.contextRunner.with(hideConnectionPools())
			.withPropertyValues("spring.datasource.url:")
			.run((context) -> assertThat(context).getBean(DataSource.class).isInstanceOf(EmbeddedDatabase.class));
	}

	@Test
	void whenNoInitializationRelatedSpringDataSourcePropertiesAreConfiguredThenInitializationBacksOff() {
		this.contextRunner
			.run((context) -> assertThat(context).doesNotHaveBean(DataSourceScriptDatabaseInitializer.class));
	}

	@Test
	void definesPropertiesBasedConnectionDetailsByDefault() {
		this.contextRunner.run((context) -> assertThat(context).hasSingleBean(PropertiesJdbcConnectionDetails.class));
	}

	@Test
	void dbcp2UsesCustomConnectionDetailsWhenDefined() {
		ApplicationContextRunner runner = new ApplicationContextRunner()
			.withPropertyValues("spring.datasource.type=org.apache.commons.dbcp2.BasicDataSource",
					"spring.datasource.dbcp2.url=jdbc:broken", "spring.datasource.dbcp2.username=alice",
					"spring.datasource.dbcp2.password=secret")
			.withConfiguration(AutoConfigurations.of(DataSourceAutoConfiguration.class))
			.withBean(JdbcConnectionDetails.class, TestJdbcConnectionDetails::new);
		runner.run((context) -> {
			assertThat(context).hasSingleBean(JdbcConnectionDetails.class)
				.doesNotHaveBean(PropertiesJdbcConnectionDetails.class);
			DataSource dataSource = context.getBean(DataSource.class);
			assertThat(dataSource).asInstanceOf(InstanceOfAssertFactories.type(BasicDataSource.class))
				.satisfies((dbcp2) -> {
					assertThat(dbcp2.getUsername()).isEqualTo("user-1");
					assertThat(dbcp2.getPassword()).isEqualTo("password-1");
					assertThat(dbcp2.getDriverClassName()).isEqualTo(DatabaseDriver.POSTGRESQL.getDriverClassName());
					assertThat(dbcp2.getUrl()).isEqualTo("jdbc:customdb://customdb.example.com:12345/database-1");
				});
		});
	}

	@Test
	void genericUsesCustomJdbcConnectionDetailsWhenAvailable() {
		ApplicationContextRunner runner = new ApplicationContextRunner()
			.withPropertyValues("spring.datasource.type=" + TestDataSource.class.getName())
			.withConfiguration(AutoConfigurations.of(DataSourceAutoConfiguration.class))
			.withBean(JdbcConnectionDetails.class, TestJdbcConnectionDetails::new);
		runner.run((context) -> {
			assertThat(context).hasSingleBean(JdbcConnectionDetails.class)
				.doesNotHaveBean(PropertiesJdbcConnectionDetails.class);
			DataSource dataSource = context.getBean(DataSource.class);
			assertThat(dataSource).isInstanceOf(TestDataSource.class);
			TestDataSource source = (TestDataSource) dataSource;
			assertThat(source.getUsername()).isEqualTo("user-1");
			assertThat(source.getPassword()).isEqualTo("password-1");
			assertThat(source.getDriver().getClass().getName())
				.isEqualTo(DatabaseDriver.POSTGRESQL.getDriverClassName());
			assertThat(source.getUrl()).isEqualTo("jdbc:customdb://customdb.example.com:12345/database-1");
		});
	}

	private static Function<ApplicationContextRunner, ApplicationContextRunner> hideConnectionPools() {
		return (runner) -> runner.withClassLoader(new FilteredClassLoader("org.apache.tomcat", "com.zaxxer.hikari",
				"org.apache.commons.dbcp2", "oracle.ucp.jdbc", "com.mchange"));
	}

	private <T extends DataSource> void assertDataSource(Class<T> expectedType, List<String> hiddenPackages,
			Consumer<T> consumer) {
		FilteredClassLoader classLoader = new FilteredClassLoader(StringUtils.toStringArray(hiddenPackages));
		this.contextRunner.withClassLoader(classLoader).run((context) -> {
			DataSource bean = context.getBean(DataSource.class);
			assertThat(bean).isInstanceOf(expectedType);
			consumer.accept(expectedType.cast(bean));
		});
	}

	@Configuration(proxyBeanMethods = false)
	static class JdbcConnectionDetailsConfiguration {

		@Bean
		JdbcConnectionDetails sqlJdbcConnectionDetails() {
			return new TestJdbcConnectionDetails();
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class TestDataSourceConfiguration {

		private BasicDataSource pool;

		@Bean
		DataSource dataSource() {
			this.pool = new BasicDataSource();
			this.pool.setDriverClassName("org.hsqldb.jdbcDriver");
			this.pool.setUrl("jdbc:hsqldb:mem:overridedb");
			this.pool.setUsername("sa");
			return this.pool;
		}

	}

	// see testExplicitDriverClassClearsUsername
	public static class DatabaseTestDriver implements Driver {

		@Override
		public Connection connect(String url, Properties info) {
			return mock(Connection.class);
		}

		@Override
		public boolean acceptsURL(String url) {
			return true;
		}

		@Override
		public DriverPropertyInfo[] getPropertyInfo(String url, Properties info) {
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
		public Logger getParentLogger() {
			return mock(Logger.class);
		}

	}

	static class DisableEmbeddedDatabaseClassLoader extends URLClassLoader {

		DisableEmbeddedDatabaseClassLoader() {
			super(new URL[0], DisableEmbeddedDatabaseClassLoader.class.getClassLoader());
		}

		@Override
		protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
			for (EmbeddedDatabaseConnection candidate : EmbeddedDatabaseConnection.values()) {
				if (name.equals(candidate.getDriverClassName())) {
					throw new ClassNotFoundException();
				}
			}
			return super.loadClass(name, resolve);
		}

	}

}
