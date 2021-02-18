/*
 * Copyright 2012-2021 the original author or authors.
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

package org.springframework.boot.jdbc;

import java.io.Closeable;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.sql.SQLException;
import java.util.Arrays;

import javax.sql.DataSource;

import com.zaxxer.hikari.HikariDataSource;
import oracle.jdbc.pool.OracleDataSource;
import oracle.ucp.jdbc.PoolDataSourceImpl;
import org.apache.commons.dbcp2.BasicDataSource;
import org.h2.Driver;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.postgresql.ds.PGSimpleDataSource;

import org.springframework.jdbc.datasource.SimpleDriverDataSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link DataSourceBuilder}.
 *
 * @author Stephane Nicoll
 * @author Fabio Grassi
 */
class DataSourceBuilderTests {

	private DataSource dataSource;

	@AfterEach
	void shutdownDataSource() throws IOException {
		if (this.dataSource instanceof Closeable) {
			((Closeable) this.dataSource).close();
		}
	}

	@Test
	void defaultToHikari() {
		this.dataSource = DataSourceBuilder.create().url("jdbc:h2:test").build();
		assertThat(this.dataSource).isInstanceOf(HikariDataSource.class);
		HikariDataSource hikariDataSource = (HikariDataSource) this.dataSource;
		assertThat(hikariDataSource.getJdbcUrl()).isEqualTo("jdbc:h2:test");
	}

	@Test
	void defaultToTomcatIfHikariIsNotAvailable() {
		this.dataSource = DataSourceBuilder.create(new HidePackagesClassLoader("com.zaxxer.hikari")).url("jdbc:h2:test")
				.build();
		assertThat(this.dataSource).isInstanceOf(org.apache.tomcat.jdbc.pool.DataSource.class);
	}

	@Test
	void defaultToCommonsDbcp2IfNeitherHikariNorTomcatIsNotAvailable() {
		this.dataSource = DataSourceBuilder
				.create(new HidePackagesClassLoader("com.zaxxer.hikari", "org.apache.tomcat.jdbc.pool"))
				.url("jdbc:h2:test").build();
		assertThat(this.dataSource).isInstanceOf(BasicDataSource.class);
	}

	@Test
	void defaultToOracleUcpAsLastResort() {
		this.dataSource = DataSourceBuilder.create(new HidePackagesClassLoader("com.zaxxer.hikari",
				"org.apache.tomcat.jdbc.pool", "org.apache.commons.dbcp2")).url("jdbc:h2:test").build();
		assertThat(this.dataSource).isInstanceOf(PoolDataSourceImpl.class);
	}

	@Test
	void specificTypeOfDataSource() {
		HikariDataSource hikariDataSource = DataSourceBuilder.create().type(HikariDataSource.class).build();
		assertThat(hikariDataSource).isInstanceOf(HikariDataSource.class);
	}

	@Test
	void dataSourceCanBeCreatedWithSimpleDriverDataSource() {
		this.dataSource = DataSourceBuilder.create().url("jdbc:h2:test").type(SimpleDriverDataSource.class).build();
		assertThat(this.dataSource).isInstanceOf(SimpleDriverDataSource.class);
		SimpleDriverDataSource simpleDriverDataSource = (SimpleDriverDataSource) this.dataSource;
		assertThat(simpleDriverDataSource.getUrl()).isEqualTo("jdbc:h2:test");
		assertThat(simpleDriverDataSource.getDriver()).isInstanceOf(Driver.class);
	}

	@Test
	void dataSourceCanBeCreatedWithOracleDataSource() throws SQLException {
		this.dataSource = DataSourceBuilder.create().url("jdbc:oracle:thin:@localhost:1521:xe")
				.type(OracleDataSource.class).username("test").build();
		assertThat(this.dataSource).isInstanceOf(OracleDataSource.class);
		OracleDataSource oracleDataSource = (OracleDataSource) this.dataSource;
		assertThat(oracleDataSource.getURL()).isEqualTo("jdbc:oracle:thin:@localhost:1521:xe");
		assertThat(oracleDataSource.getUser()).isEqualTo("test");
	}

	@Test
	void dataSourceCanBeCreatedWithOracleUcpDataSource() {
		this.dataSource = DataSourceBuilder.create().driverClassName("org.hsqldb.jdbc.JDBCDriver")
				.type(PoolDataSourceImpl.class).username("test").build();
		assertThat(this.dataSource).isInstanceOf(PoolDataSourceImpl.class);
		PoolDataSourceImpl upcDataSource = (PoolDataSourceImpl) this.dataSource;
		assertThat(upcDataSource.getConnectionFactoryClassName()).isEqualTo("org.hsqldb.jdbc.JDBCDriver");
		assertThat(upcDataSource.getUser()).isEqualTo("test");
	}

	@Test
	void dataSourceCanBeCreatedWithH2JdbcDataSource() {
		this.dataSource = DataSourceBuilder.create().url("jdbc:h2:test").type(JdbcDataSource.class).username("test")
				.build();
		assertThat(this.dataSource).isInstanceOf(JdbcDataSource.class);
		JdbcDataSource h2DataSource = (JdbcDataSource) this.dataSource;
		assertThat(h2DataSource.getUser()).isEqualTo("test");
	}

	@Test
	void dataSourceCanBeCreatedWithPGDataSource() {
		this.dataSource = DataSourceBuilder.create().url("jdbc:postgresql://localhost/test")
				.type(PGSimpleDataSource.class).username("test").build();
		assertThat(this.dataSource).isInstanceOf(PGSimpleDataSource.class);
		PGSimpleDataSource pgDataSource = (PGSimpleDataSource) this.dataSource;
		assertThat(pgDataSource.getUser()).isEqualTo("test");
	}

	@Test
	void dataSourceAliasesAreOnlyAppliedToRelevantDataSource() {
		this.dataSource = DataSourceBuilder.create().url("jdbc:h2:test").type(TestDataSource.class).username("test")
				.build();
		assertThat(this.dataSource).isInstanceOf(TestDataSource.class);
		TestDataSource testDataSource = (TestDataSource) this.dataSource;
		assertThat(testDataSource.getUrl()).isEqualTo("jdbc:h2:test");
		assertThat(testDataSource.getJdbcUrl()).isNull();
		assertThat(testDataSource.getUsername()).isEqualTo("test");
		assertThat(testDataSource.getUser()).isNull();
		assertThat(testDataSource.getDriverClassName()).isEqualTo(Driver.class.getName());
		assertThat(testDataSource.getDriverClass()).isNull();
	}

	final class HidePackagesClassLoader extends URLClassLoader {

		private final String[] hiddenPackages;

		HidePackagesClassLoader(String... hiddenPackages) {
			super(new URL[0], HidePackagesClassLoader.class.getClassLoader());
			this.hiddenPackages = hiddenPackages;
		}

		@Override
		protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
			if (Arrays.stream(this.hiddenPackages).anyMatch(name::startsWith)) {
				throw new ClassNotFoundException();
			}
			return super.loadClass(name, resolve);
		}

	}

	public static class TestDataSource extends org.apache.tomcat.jdbc.pool.DataSource {

		private String jdbcUrl;

		private String user;

		private String driverClass;

		public String getJdbcUrl() {
			return this.jdbcUrl;
		}

		public void setJdbcUrl(String jdbcUrl) {
			this.jdbcUrl = jdbcUrl;
		}

		public String getUser() {
			return this.user;
		}

		public void setUser(String user) {
			this.user = user;
		}

		public String getDriverClass() {
			return this.driverClass;
		}

		public void setDriverClass(String driverClass) {
			this.driverClass = driverClass;
		}

	}

}
