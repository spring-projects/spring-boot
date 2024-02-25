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
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.boot.jdbc.DatabaseDriver;
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

	/**
     * Creates a data source using the provided JDBC connection details, type, and class loader.
     * 
     * @param connectionDetails the JDBC connection details
     * @param type the type of the data source
     * @param classLoader the class loader to use for creating the data source
     * @return the created data source
     */
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

		/**
         * Creates a TomcatJdbcConnectionDetailsBeanPostProcessor bean if there is no existing bean of type PropertiesJdbcConnectionDetails.
         * This bean post processor is responsible for configuring the Tomcat JDBC connection details.
         * 
         * @param connectionDetailsProvider the provider for the JDBC connection details
         * @return the TomcatJdbcConnectionDetailsBeanPostProcessor bean
         */
        @Bean
		@ConditionalOnMissingBean(PropertiesJdbcConnectionDetails.class)
		static TomcatJdbcConnectionDetailsBeanPostProcessor tomcatJdbcConnectionDetailsBeanPostProcessor(
				ObjectProvider<JdbcConnectionDetails> connectionDetailsProvider) {
			return new TomcatJdbcConnectionDetailsBeanPostProcessor(connectionDetailsProvider);
		}

		/**
         * Creates a Tomcat JDBC DataSource using the provided properties and connection details.
         * 
         * @param properties the DataSourceProperties object containing the configuration properties
         * @param connectionDetails the JdbcConnectionDetails object containing the connection details
         * @return a Tomcat JDBC DataSource
         */
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

		/**
         * Creates a new HikariJdbcConnectionDetailsBeanPostProcessor bean post processor.
         * 
         * @param connectionDetailsProvider the provider for the JdbcConnectionDetails bean
         * @return the HikariJdbcConnectionDetailsBeanPostProcessor instance
         */
        @Bean
		static HikariJdbcConnectionDetailsBeanPostProcessor jdbcConnectionDetailsHikariBeanPostProcessor(
				ObjectProvider<JdbcConnectionDetails> connectionDetailsProvider) {
			return new HikariJdbcConnectionDetailsBeanPostProcessor(connectionDetailsProvider);
		}

		/**
         * Creates a HikariDataSource using the provided DataSourceProperties and JdbcConnectionDetails.
         * 
         * @param properties the DataSourceProperties containing the configuration properties for the data source
         * @param connectionDetails the JdbcConnectionDetails containing the connection details for the data source
         * @return a HikariDataSource configured with the provided properties and connection details
         */
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

		/**
         * Creates a new instance of Dbcp2JdbcConnectionDetailsBeanPostProcessor.
         * 
         * @param connectionDetailsProvider the provider for JdbcConnectionDetails
         * @return the Dbcp2JdbcConnectionDetailsBeanPostProcessor instance
         */
        @Bean
		static Dbcp2JdbcConnectionDetailsBeanPostProcessor dbcp2JdbcConnectionDetailsBeanPostProcessor(
				ObjectProvider<JdbcConnectionDetails> connectionDetailsProvider) {
			return new Dbcp2JdbcConnectionDetailsBeanPostProcessor(connectionDetailsProvider);
		}

		/**
         * Creates a {@link org.apache.commons.dbcp2.BasicDataSource} using the provided properties and connection details.
         * 
         * @param properties the {@link DataSourceProperties} containing the configuration properties for the data source
         * @param connectionDetails the {@link JdbcConnectionDetails} containing the connection details for the data source
         * @return a {@link org.apache.commons.dbcp2.BasicDataSource} configured with the provided properties and connection details
         */
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

		/**
         * Creates a new instance of OracleUcpJdbcConnectionDetailsBeanPostProcessor.
         * 
         * @param connectionDetailsProvider the provider for the JdbcConnectionDetails
         * @return the OracleUcpJdbcConnectionDetailsBeanPostProcessor instance
         */
        @Bean
		static OracleUcpJdbcConnectionDetailsBeanPostProcessor oracleUcpJdbcConnectionDetailsBeanPostProcessor(
				ObjectProvider<JdbcConnectionDetails> connectionDetailsProvider) {
			return new OracleUcpJdbcConnectionDetailsBeanPostProcessor(connectionDetailsProvider);
		}

		/**
         * Creates a data source using the provided connection details and properties.
         * 
         * @param properties the data source properties
         * @param connectionDetails the JDBC connection details
         * @return the created data source
         * @throws SQLException if an error occurs while creating the data source
         */
        @Bean
		@ConfigurationProperties(prefix = "spring.datasource.oracleucp")
		PoolDataSourceImpl dataSource(DataSourceProperties properties, JdbcConnectionDetails connectionDetails)
				throws SQLException {
			PoolDataSourceImpl dataSource = createDataSource(connectionDetails, PoolDataSourceImpl.class,
					properties.getClassLoader());
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

		/**
         * Creates a data source using the provided connection details and properties.
         * 
         * @param properties the properties of the data source
         * @param connectionDetails the connection details for the data source
         * @return the created data source
         */
        @Bean
		DataSource dataSource(DataSourceProperties properties, JdbcConnectionDetails connectionDetails) {
			return createDataSource(connectionDetails, properties.getType(), properties.getClassLoader());
		}

	}

}
