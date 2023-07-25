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

import java.sql.SQLException;

import javax.sql.DataSource;

import com.zaxxer.hikari.HikariDataSource;
import oracle.jdbc.OracleConnection;
import oracle.ucp.jdbc.PoolDataSourceImpl;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnCheckpointRestore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.boot.jdbc.DatabaseDriver;
import org.springframework.boot.jdbc.HikariCheckpointRestoreLifecycle;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

/**
 * Actual DataSource configurations imported by {@link DataSourceAutoConfiguration}.
 *
 * @author Dave Syer
 * @author Phillip Webb
 * @author Stephane Nicoll
 * @author Fabio Grassi
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 */
abstract class DataSourceConfiguration {

	@SuppressWarnings("unchecked")
	private static <T> T createDataSource(JdbcConnectionDetails connectionDetails, Class<? extends DataSource> type,
			ClassLoader classLoader) {
		return (T) DataSourceBuilder.create(classLoader)
			.type(type)
			.driverClassName(connectionDetails.getDriverClassName())
			.url(connectionDetails.getJdbcUrl())
			.username(connectionDetails.getUsername())
			.password(connectionDetails.getPassword())
			.build();
	}

	/**
	 * Tomcat Pool DataSource configuration.
	 */
	@Configuration(proxyBeanMethods = false)
	@ConditionalOnClass(org.apache.tomcat.jdbc.pool.DataSource.class)
	@ConditionalOnMissingBean(DataSource.class)
	@ConditionalOnProperty(name = "spring.datasource.type", havingValue = "org.apache.tomcat.jdbc.pool.DataSource",
			matchIfMissing = true)
	static class Tomcat {

		@Bean
		@ConditionalOnMissingBean(PropertiesJdbcConnectionDetails.class)
		static TomcatJdbcConnectionDetailsBeanPostProcessor tomcatJdbcConnectionDetailsBeanPostProcessor(
				ObjectProvider<JdbcConnectionDetails> connectionDetailsProvider) {
			return new TomcatJdbcConnectionDetailsBeanPostProcessor(connectionDetailsProvider);
		}

		@Bean
		@ConfigurationProperties(prefix = "spring.datasource.tomcat")
		org.apache.tomcat.jdbc.pool.DataSource dataSource(DataSourceProperties properties,
				JdbcConnectionDetails connectionDetails) {
			Class<? extends DataSource> dataSourceType = org.apache.tomcat.jdbc.pool.DataSource.class;
			org.apache.tomcat.jdbc.pool.DataSource dataSource = createDataSource(connectionDetails, dataSourceType,
					properties.getClassLoader());
			String validationQuery;
			DatabaseDriver databaseDriver = DatabaseDriver.fromJdbcUrl(connectionDetails.getJdbcUrl());
			validationQuery = databaseDriver.getValidationQuery();
			if (validationQuery != null) {
				dataSource.setTestOnBorrow(true);
				dataSource.setValidationQuery(validationQuery);
			}
			return dataSource;
		}

	}

	/**
	 * Hikari DataSource configuration.
	 */
	@Configuration(proxyBeanMethods = false)
	@ConditionalOnClass(HikariDataSource.class)
	@ConditionalOnMissingBean(DataSource.class)
	@ConditionalOnProperty(name = "spring.datasource.type", havingValue = "com.zaxxer.hikari.HikariDataSource",
			matchIfMissing = true)
	static class Hikari {

		@Bean
		static HikariJdbcConnectionDetailsBeanPostProcessor jdbcConnectionDetailsHikariBeanPostProcessor(
				ObjectProvider<JdbcConnectionDetails> connectionDetailsProvider) {
			return new HikariJdbcConnectionDetailsBeanPostProcessor(connectionDetailsProvider);
		}

		@Bean
		@ConfigurationProperties(prefix = "spring.datasource.hikari")
		HikariDataSource dataSource(DataSourceProperties properties, JdbcConnectionDetails connectionDetails) {
			HikariDataSource dataSource = createDataSource(connectionDetails, HikariDataSource.class,
					properties.getClassLoader());
			if (StringUtils.hasText(properties.getName())) {
				dataSource.setPoolName(properties.getName());
			}
			return dataSource;
		}

		@Bean
		@ConditionalOnCheckpointRestore
		HikariCheckpointRestoreLifecycle hikariCheckpointRestoreLifecycle(HikariDataSource hikariDataSource) {
			return new HikariCheckpointRestoreLifecycle(hikariDataSource);
		}

	}

	/**
	 * DBCP DataSource configuration.
	 */
	@Configuration(proxyBeanMethods = false)
	@ConditionalOnClass(org.apache.commons.dbcp2.BasicDataSource.class)
	@ConditionalOnMissingBean(DataSource.class)
	@ConditionalOnProperty(name = "spring.datasource.type", havingValue = "org.apache.commons.dbcp2.BasicDataSource",
			matchIfMissing = true)
	static class Dbcp2 {

		@Bean
		static Dbcp2JdbcConnectionDetailsBeanPostProcessor dbcp2JdbcConnectionDetailsBeanPostProcessor(
				ObjectProvider<JdbcConnectionDetails> connectionDetailsProvider) {
			return new Dbcp2JdbcConnectionDetailsBeanPostProcessor(connectionDetailsProvider);
		}

		@Bean
		@ConfigurationProperties(prefix = "spring.datasource.dbcp2")
		org.apache.commons.dbcp2.BasicDataSource dataSource(DataSourceProperties properties,
				JdbcConnectionDetails connectionDetails) {
			Class<? extends DataSource> dataSourceType = org.apache.commons.dbcp2.BasicDataSource.class;
			return createDataSource(connectionDetails, dataSourceType, properties.getClassLoader());
		}

	}

	/**
	 * Oracle UCP DataSource configuration.
	 */
	@Configuration(proxyBeanMethods = false)
	@ConditionalOnClass({ PoolDataSourceImpl.class, OracleConnection.class })
	@ConditionalOnMissingBean(DataSource.class)
	@ConditionalOnProperty(name = "spring.datasource.type", havingValue = "oracle.ucp.jdbc.PoolDataSource",
			matchIfMissing = true)
	static class OracleUcp {

		@Bean
		static OracleUcpJdbcConnectionDetailsBeanPostProcessor oracleUcpJdbcConnectionDetailsBeanPostProcessor(
				ObjectProvider<JdbcConnectionDetails> connectionDetailsProvider) {
			return new OracleUcpJdbcConnectionDetailsBeanPostProcessor(connectionDetailsProvider);
		}

		@Bean
		@ConfigurationProperties(prefix = "spring.datasource.oracleucp")
		PoolDataSourceImpl dataSource(DataSourceProperties properties, JdbcConnectionDetails connectionDetails)
				throws SQLException {
			PoolDataSourceImpl dataSource = createDataSource(connectionDetails, PoolDataSourceImpl.class,
					properties.getClassLoader());
			dataSource.setValidateConnectionOnBorrow(true);
			if (StringUtils.hasText(properties.getName())) {
				dataSource.setConnectionPoolName(properties.getName());
			}
			return dataSource;
		}

	}

	/**
	 * Generic DataSource configuration.
	 */
	@Configuration(proxyBeanMethods = false)
	@ConditionalOnMissingBean(DataSource.class)
	@ConditionalOnProperty(name = "spring.datasource.type")
	static class Generic {

		@Bean
		DataSource dataSource(DataSourceProperties properties, JdbcConnectionDetails connectionDetails) {
			return createDataSource(connectionDetails, properties.getType(), properties.getClassLoader());
		}

	}

}
