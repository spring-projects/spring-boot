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

import java.sql.SQLException;

import javax.annotation.PreDestroy;
import javax.sql.DataSource;

import org.apache.commons.dbcp.BasicDataSource;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.dao.DataAccessResourceFailureException;

/**
 * Configuration for a Commons DBCP database pool. The DBCP pool is popular but not
 * recommended in high volume environments (the Tomcat DataSource is more reliable).
 * 
 * @author Dave Syer
 * @see DataSourceAutoConfiguration
 */
@Configuration
public class CommonsDataSourceConfiguration extends AbstractDataSourceConfiguration {

	private static Log logger = LogFactory.getLog(CommonsDataSourceConfiguration.class);

	private BasicDataSource pool;

	public CommonsDataSourceConfiguration() {
		// Ensure to set the correct default value for Commons DBCP
		setInitialSize(0);
	}

	@Bean(destroyMethod = "close")
	public DataSource dataSource() {
		logger.info("Hint: using Commons DBCP BasicDataSource. It's going to work, "
				+ "but the Tomcat DataSource is more reliable.");
		this.pool = createAndConfigurePool();
		return this.pool;
	}

	private BasicDataSource createAndConfigurePool() {
		BasicDataSource pool = new BasicDataSource();
		pool.setDriverClassName(getDriverClassName());
		pool.setUrl(getUrl());
		if (getUsername() != null) {
			pool.setUsername(getUsername());
		}
		if (getPassword() != null) {
			pool.setPassword(getPassword());
		}
		pool.setInitialSize(getInitialSize());
		pool.setMaxActive(getMaxActive());
		pool.setMaxIdle(getMaxIdle());
		pool.setMinIdle(getMinIdle());
		pool.setTestOnBorrow(isTestOnBorrow());
		pool.setTestOnReturn(isTestOnReturn());
		pool.setTestWhileIdle(isTestWhileIdle());
		pool.setValidationQuery(getValidationQuery());
		if (getTimeBetweenEvictionRunsMillis() != null) {
			pool.setTimeBetweenEvictionRunsMillis(getTimeBetweenEvictionRunsMillis());
		}
		if (getMinEvictableIdleTimeMillis() != null) {
			pool.setMinEvictableIdleTimeMillis(getMinEvictableIdleTimeMillis());
		}
		if (getMaxWaitMillis() != null) {
			pool.setMaxWait(getMaxWaitMillis());
		}
		return pool;
	}

	@PreDestroy
	public void close() {
		if (this.pool != null) {
			try {
				this.pool.close();
			}
			catch (SQLException ex) {
				throw new DataAccessResourceFailureException(
						"Could not close data source", ex);
			}
		}
	}

}
