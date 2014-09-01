/*
 * Copyright 2012-2014 the original author or authors.
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

import org.apache.commons.dbcp.BasicDataSource;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.zaxxer.hikari.HikariDataSource;

/**
 * Register the {@link DataSourceMetadataProvider} instances for the supported data
 * sources.
 *
 * @author Stephane Nicoll
 * @since 1.2.0
 */
@Configuration
public class DataSourceMetadataProvidersConfiguration {

	@Configuration
	@ConditionalOnClass(org.apache.tomcat.jdbc.pool.DataSource.class)
	static class TomcatDataSourceProviderConfiguration {

		@Bean
		public DataSourceMetadataProvider tomcatDataSourceProvider() {
			return new DataSourceMetadataProvider() {
				@Override
				public DataSourceMetadata getDataSourceMetadata(DataSource dataSource) {
					if (dataSource instanceof org.apache.tomcat.jdbc.pool.DataSource) {
						return new TomcatDataSourceMetadata(
								(org.apache.tomcat.jdbc.pool.DataSource) dataSource);
					}
					return null;
				}
			};
		}

	}

	@Configuration
	@ConditionalOnClass(HikariDataSource.class)
	static class HikariDataSourceProviderConfiguration {

		@Bean
		public DataSourceMetadataProvider hikariDataSourceProvider() {
			return new DataSourceMetadataProvider() {
				@Override
				public DataSourceMetadata getDataSourceMetadata(DataSource dataSource) {
					if (dataSource instanceof HikariDataSource) {
						return new HikariDataSourceMetadata((HikariDataSource) dataSource);
					}
					return null;
				}
			};
		}

	}

	@Configuration
	@ConditionalOnClass(BasicDataSource.class)
	static class CommonsDbcpDataSourceProviderConfiguration {

		@Bean
		public DataSourceMetadataProvider commonsDbcpDataSourceProvider() {
			return new DataSourceMetadataProvider() {
				@Override
				public DataSourceMetadata getDataSourceMetadata(DataSource dataSource) {
					if (dataSource instanceof BasicDataSource) {
						return new CommonsDbcpDataSourceMetadata(
								(BasicDataSource) dataSource);
					}
					return null;
				}
			};
		}

	}

}
