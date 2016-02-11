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

package org.springframework.boot.devtools.autoconfigure;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import javax.sql.DataSource;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabase;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for DevTools-specific
 * {@link DataSource} configuration.
 *
 * @author Andy Wilkinson
 * @since 1.3.3
 */
@AutoConfigureAfter(DataSourceAutoConfiguration.class)
@ConditionalOnBean({ DataSource.class, DataSourceProperties.class })
@Configuration
public class DevToolsDataSourceAutoConfiguration {

	@Bean
	NonEmbeddedInMemoryDatabaseShutdownExecutor inMemoryDatabaseShutdownExecutor(
			DataSource dataSource, DataSourceProperties dataSourceProperties) {
		return new NonEmbeddedInMemoryDatabaseShutdownExecutor(dataSource,
				dataSourceProperties);
	}

	static final class NonEmbeddedInMemoryDatabaseShutdownExecutor
			implements DisposableBean {

		private static final Set<String> IN_MEMORY_DRIVER_CLASS_NAMES = new HashSet<String>(
				Arrays.asList("org.apache.derby.jdbc.EmbeddedDriver", "org.h2.Driver",
						"org.h2.jdbcx.JdbcDataSource", "org.hsqldb.jdbcDriver",
						"org.hsqldb.jdbc.JDBCDriver",
						"org.hsqldb.jdbc.pool.JDBCXADataSource"));

		private final DataSource dataSource;

		private final DataSourceProperties dataSourceProperties;

		NonEmbeddedInMemoryDatabaseShutdownExecutor(DataSource dataSource,
				DataSourceProperties dataSourceProperties) {
			this.dataSource = dataSource;
			this.dataSourceProperties = dataSourceProperties;
		}

		@Override
		public void destroy() throws Exception {
			if (dataSourceRequiresShutdown()) {
				this.dataSource.getConnection().createStatement().execute("SHUTDOWN");
			}
		}

		private boolean dataSourceRequiresShutdown() {
			return IN_MEMORY_DRIVER_CLASS_NAMES
					.contains(this.dataSourceProperties.getDriverClassName())
					&& (!(this.dataSource instanceof EmbeddedDatabase));
		}

	}

}
