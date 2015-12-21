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

import com.zaxxer.hikari.HikariDataSource;
import org.apache.commons.dbcp.BasicDataSource;
import org.apache.tomcat.jdbc.pool.DataSource;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Expose the metadata of the supported data sources. Only used to harvest the relevant
 * properties metadata.
 *
 * @author Stephane Nicoll
 * @since 1.2.0
 */
class DataSourceConfigMetadata {

	@ConfigurationProperties(DataSourceProperties.PREFIX)
	public DataSource tomcatDataSource() {
		return (DataSource) DataSourceBuilder.create().type(DataSource.class).build();
	}

	@ConfigurationProperties(DataSourceProperties.PREFIX)
	public HikariDataSource hikariDataSource() {
		return (HikariDataSource) DataSourceBuilder.create().type(HikariDataSource.class)
				.build();
	}

	@ConfigurationProperties(DataSourceProperties.PREFIX)
	public BasicDataSource dbcpDataSource() {
		return (BasicDataSource) DataSourceBuilder.create().type(BasicDataSource.class)
				.build();
	}

}
