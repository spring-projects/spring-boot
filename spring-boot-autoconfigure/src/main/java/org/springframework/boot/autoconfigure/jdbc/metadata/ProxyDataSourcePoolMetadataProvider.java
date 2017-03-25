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

package org.springframework.boot.autoconfigure.jdbc.metadata;

import javax.sql.DataSource;

import com.zaxxer.hikari.HikariDataSource;

import org.springframework.boot.autoconfigure.jdbc.ProxyDataSourceUtil;
import org.springframework.util.ClassUtils;

/**
 * {@link DataSourcePoolMetadataProvider} that returns specific implementation for a real
 * {@link DataSource} extracted from a proxy {@link DataSource}.
 *
 * @author Arthur Gavlyukovskiy
 * @since 1.5.3
 */
class ProxyDataSourcePoolMetadataProvider implements DataSourcePoolMetadataProvider {

	/**
	 * Returns implementation for a real {@link DataSource}, returns null if a real
	 * {@link DataSource} could not be extracted or {@link DataSourcePoolMetadata}
	 * implementation can not be found for that {@link DataSource}.
	 *
	 * @param dataSource data source
	 * @return {@link DataSourcePoolMetadata} for a real data source or null
	 */
	@Override
	public DataSourcePoolMetadata getDataSourcePoolMetadata(DataSource dataSource) {
		DataSource realDataSource = ProxyDataSourceUtil.tryFindRealDataSource(dataSource);
		if (dataSource == realDataSource) {
			return null;
		}
		ClassLoader classLoader = dataSource.getClass().getClassLoader();
		if (ClassUtils.isPresent("com.zaxxer.hikari.HikariDataSource", classLoader)
				&& realDataSource instanceof HikariDataSource) {
			return new HikariDataSourcePoolMetadata((HikariDataSource) realDataSource);
		}
		else if (ClassUtils.isPresent("org.apache.tomcat.jdbc.pool.DataSource", classLoader)
				&& realDataSource instanceof org.apache.tomcat.jdbc.pool.DataSource) {
			return new TomcatDataSourcePoolMetadata((org.apache.tomcat.jdbc.pool.DataSource) realDataSource);
		}
		else if (ClassUtils.isPresent("org.apache.commons.dbcp.BasicDataSource", classLoader)
				&& realDataSource instanceof org.apache.commons.dbcp.BasicDataSource) {
			return new CommonsDbcpDataSourcePoolMetadata((org.apache.commons.dbcp.BasicDataSource) realDataSource);
		}
		else if (ClassUtils.isPresent("org.apache.commons.dbcp2.BasicDataSource", classLoader)
				&& realDataSource instanceof org.apache.commons.dbcp2.BasicDataSource) {
			return new CommonsDbcp2DataSourcePoolMetadata((org.apache.commons.dbcp2.BasicDataSource) realDataSource);
		}
		return null;
	}
}
