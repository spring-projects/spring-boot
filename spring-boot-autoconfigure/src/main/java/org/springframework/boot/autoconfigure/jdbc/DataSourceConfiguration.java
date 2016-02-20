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

import javax.sql.DataSource;

import com.zaxxer.hikari.HikariDataSource;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DatabaseDriver;
import org.springframework.context.annotation.Bean;

/**
 * Actual DataSource configurations imported by {@link DataSourceAutoConfiguration}.
 *
 * @author Dave Syer
 * @author Phillip Webb
 * @author Stephane Nicoll
 */
abstract class DataSourceConfiguration {

	@SuppressWarnings("unchecked")
	protected <T> T createDataSource(DataSourceProperties properties,
			Class<? extends DataSource> type) {
		return (T) DataSourceBuilder.create(properties.getClassLoader()).type(type)
				.driverClassName(properties.determineDriverClassName())
				.url(properties.determineUrl()).username(properties.determineUsername())
				.password(properties.determinePassword()).build();
	}

	@ConditionalOnClass(org.apache.tomcat.jdbc.pool.DataSource.class)
	@ConditionalOnProperty(name = "spring.datasource.type", havingValue = "org.apache.tomcat.jdbc.pool.DataSource", matchIfMissing = true)
	static class Tomcat extends DataSourceConfiguration {

		@Bean
		@ConfigurationProperties("spring.datasource.tomcat")
		public org.apache.tomcat.jdbc.pool.DataSource dataSource(
				DataSourceProperties properties) {
			org.apache.tomcat.jdbc.pool.DataSource dataSource = createDataSource(
					properties, org.apache.tomcat.jdbc.pool.DataSource.class);
			DatabaseDriver databaseDriver = DatabaseDriver
					.fromJdbcUrl(properties.determineUrl());
			String validationQuery = databaseDriver.getValidationQuery();
			if (validationQuery != null) {
				dataSource.setTestOnBorrow(true);
				dataSource.setValidationQuery(validationQuery);
			}
			return dataSource;
		}

	}

	@ConditionalOnClass(HikariDataSource.class)
	@ConditionalOnProperty(name = "spring.datasource.type", havingValue = "com.zaxxer.hikari.HikariDataSource", matchIfMissing = true)
	static class Hikari extends DataSourceConfiguration {

		@Bean
		@ConfigurationProperties("spring.datasource.hikari")
		public HikariDataSource dataSource(DataSourceProperties properties) {
			return createDataSource(properties, HikariDataSource.class);
		}
	}

	@ConditionalOnClass(org.apache.commons.dbcp.BasicDataSource.class)
	@ConditionalOnProperty(name = "spring.datasource.type", havingValue = "org.apache.commons.dbcp.BasicDataSource", matchIfMissing = true)
	static class Dbcp extends DataSourceConfiguration {

		@Bean
		@ConfigurationProperties("spring.datasource.dbcp")
		public org.apache.commons.dbcp.BasicDataSource dataSource(
				DataSourceProperties properties) {
			org.apache.commons.dbcp.BasicDataSource dataSource = createDataSource(
					properties, org.apache.commons.dbcp.BasicDataSource.class);
			DatabaseDriver databaseDriver = DatabaseDriver
					.fromJdbcUrl(properties.determineUrl());
			String validationQuery = databaseDriver.getValidationQuery();
			if (validationQuery != null) {
				dataSource.setTestOnBorrow(true);
				dataSource.setValidationQuery(validationQuery);
			}
			return dataSource;
		}
	}

	@ConditionalOnClass(org.apache.commons.dbcp2.BasicDataSource.class)
	@ConditionalOnProperty(name = "spring.datasource.type", havingValue = "org.apache.commons.dbcp2.BasicDataSource", matchIfMissing = true)
	static class Dbcp2 extends DataSourceConfiguration {

		@Bean
		@ConfigurationProperties("spring.datasource.dbcp2")
		public org.apache.commons.dbcp2.BasicDataSource dataSource(
				DataSourceProperties properties) {
			return createDataSource(properties,
					org.apache.commons.dbcp2.BasicDataSource.class);
		}
	}

}
