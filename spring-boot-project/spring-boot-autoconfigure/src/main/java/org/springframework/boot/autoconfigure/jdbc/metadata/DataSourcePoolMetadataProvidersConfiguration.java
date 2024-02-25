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

package org.springframework.boot.autoconfigure.jdbc.metadata;

import com.zaxxer.hikari.HikariConfigMXBean;
import com.zaxxer.hikari.HikariDataSource;
import oracle.jdbc.OracleConnection;
import oracle.ucp.jdbc.PoolDataSource;
import org.apache.commons.dbcp2.BasicDataSource;
import org.apache.commons.dbcp2.BasicDataSourceMXBean;
import org.apache.tomcat.jdbc.pool.jmx.ConnectionPoolMBean;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.jdbc.DataSourceUnwrapper;
import org.springframework.boot.jdbc.metadata.CommonsDbcp2DataSourcePoolMetadata;
import org.springframework.boot.jdbc.metadata.DataSourcePoolMetadataProvider;
import org.springframework.boot.jdbc.metadata.HikariDataSourcePoolMetadata;
import org.springframework.boot.jdbc.metadata.OracleUcpDataSourcePoolMetadata;
import org.springframework.boot.jdbc.metadata.TomcatDataSourcePoolMetadata;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Register the {@link DataSourcePoolMetadataProvider} instances for the supported data
 * sources.
 *
 * @author Stephane Nicoll
 * @author Fabio Grassi
 * @since 1.2.0
 */
@Configuration(proxyBeanMethods = false)
public class DataSourcePoolMetadataProvidersConfiguration {

	/**
	 * TomcatDataSourcePoolMetadataProviderConfiguration class.
	 */
	@Configuration(proxyBeanMethods = false)
	@ConditionalOnClass(org.apache.tomcat.jdbc.pool.DataSource.class)
	static class TomcatDataSourcePoolMetadataProviderConfiguration {

		/**
		 * Returns a DataSourcePoolMetadataProvider that provides metadata about the
		 * Tomcat JDBC connection pool DataSource.
		 * @return the DataSourcePoolMetadataProvider for the Tomcat JDBC connection pool
		 * DataSource
		 */
		@Bean
		DataSourcePoolMetadataProvider tomcatPoolDataSourceMetadataProvider() {
			return (dataSource) -> {
				org.apache.tomcat.jdbc.pool.DataSource tomcatDataSource = DataSourceUnwrapper.unwrap(dataSource,
						ConnectionPoolMBean.class, org.apache.tomcat.jdbc.pool.DataSource.class);
				if (tomcatDataSource != null) {
					return new TomcatDataSourcePoolMetadata(tomcatDataSource);
				}
				return null;
			};
		}

	}

	/**
	 * HikariPoolDataSourceMetadataProviderConfiguration class.
	 */
	@Configuration(proxyBeanMethods = false)
	@ConditionalOnClass(HikariDataSource.class)
	static class HikariPoolDataSourceMetadataProviderConfiguration {

		/**
		 * Returns a DataSourcePoolMetadataProvider for HikariCP DataSource.
		 * @return the DataSourcePoolMetadataProvider for HikariCP DataSource
		 */
		@Bean
		DataSourcePoolMetadataProvider hikariPoolDataSourceMetadataProvider() {
			return (dataSource) -> {
				HikariDataSource hikariDataSource = DataSourceUnwrapper.unwrap(dataSource, HikariConfigMXBean.class,
						HikariDataSource.class);
				if (hikariDataSource != null) {
					return new HikariDataSourcePoolMetadata(hikariDataSource);
				}
				return null;
			};
		}

	}

	/**
	 * CommonsDbcp2PoolDataSourceMetadataProviderConfiguration class.
	 */
	@Configuration(proxyBeanMethods = false)
	@ConditionalOnClass(BasicDataSource.class)
	static class CommonsDbcp2PoolDataSourceMetadataProviderConfiguration {

		/**
		 * Returns a DataSourcePoolMetadataProvider that provides metadata about the
		 * Commons DBCP2 pool DataSource.
		 * @return The DataSourcePoolMetadataProvider for the Commons DBCP2 pool
		 * DataSource.
		 */
		@Bean
		DataSourcePoolMetadataProvider commonsDbcp2PoolDataSourceMetadataProvider() {
			return (dataSource) -> {
				BasicDataSource dbcpDataSource = DataSourceUnwrapper.unwrap(dataSource, BasicDataSourceMXBean.class,
						BasicDataSource.class);
				if (dbcpDataSource != null) {
					return new CommonsDbcp2DataSourcePoolMetadata(dbcpDataSource);
				}
				return null;
			};
		}

	}

	/**
	 * OracleUcpPoolDataSourceMetadataProviderConfiguration class.
	 */
	@Configuration(proxyBeanMethods = false)
	@ConditionalOnClass({ PoolDataSource.class, OracleConnection.class })
	static class OracleUcpPoolDataSourceMetadataProviderConfiguration {

		/**
		 * Returns a DataSourcePoolMetadataProvider for Oracle UCP Pool DataSource.
		 * @return the DataSourcePoolMetadataProvider for Oracle UCP Pool DataSource
		 */
		@Bean
		DataSourcePoolMetadataProvider oracleUcpPoolDataSourceMetadataProvider() {
			return (dataSource) -> {
				PoolDataSource ucpDataSource = DataSourceUnwrapper.unwrap(dataSource, PoolDataSource.class);
				if (ucpDataSource != null) {
					return new OracleUcpDataSourcePoolMetadata(ucpDataSource);
				}
				return null;
			};
		}

	}

}
