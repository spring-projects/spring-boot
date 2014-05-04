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

import java.util.Properties;

import javax.annotation.PreDestroy;
import javax.sql.DataSource;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

import com.zaxxer.hikari.HikariDataSource;

/**
 * Configuration for a HikariCP database pool. The HikariCP pool is a popular data source
 * implementation that provides high performance as well as some useful opinionated
 * defaults. For compatibility with other DataSource implementations accepts configuration
 * via properties in "spring.datasource.*", e.g. "url", "driverClassName", "username",
 * "password" (and some others but the full list supported by the Tomcat pool is not
 * applicable). Note that the Hikari team recommends using a "dataSourceClassName" and a
 * Properties instance (specified here as "spring.datasource.hikari.*"). This makes the
 * binding potentially vendor specific, but gives you full control of all the native
 * features in the vendor's DataSource.
 * 
 * @author Dave Syer
 * @see DataSourceAutoConfiguration
 * @since 1.1.0
 */
@Configuration
public class HikariDataSourceConfiguration extends AbstractDataSourceConfiguration {

	private String dataSourceClassName;

	private String username;

	private HikariDataSource pool;

	private Properties hikari = new Properties();

	@Bean(destroyMethod = "shutdown")
	public DataSource dataSource() {
		this.pool = new HikariDataSource();
		if (this.dataSourceClassName == null) {
			this.pool.setDriverClassName(getDriverClassName());
		}
		else {
			this.pool.setDataSourceClassName(this.dataSourceClassName);
			this.pool.setDataSourceProperties(this.hikari);
		}
		this.pool.setJdbcUrl(getUrl());
		if (getUsername() != null) {
			this.pool.setUsername(getUsername());
		}
		if (getPassword() != null) {
			this.pool.setPassword(getPassword());
		}
		this.pool.setMaximumPoolSize(getMaxActive());
		this.pool.setMinimumIdle(getMinIdle());
		if (isTestOnBorrow()) {
			this.pool.setConnectionInitSql(getValidationQuery());
		}
		else {
			this.pool.setConnectionTestQuery(getValidationQuery());
		}
		if (getMaxWaitMillis() != null) {
			this.pool.setMaxLifetime(getMaxWaitMillis());
		}
		return this.pool;
	}

	@PreDestroy
	public void close() {
		if (this.pool != null) {
			this.pool.close();
		}
	}

	/**
	 * @param dataSourceClassName the dataSourceClassName to set
	 */
	public void setDataSourceClassName(String dataSourceClassName) {
		this.dataSourceClassName = dataSourceClassName;
	}

	@Override
	public void setUsername(String username) {
		this.username = username;
	}

	/**
	 * @return the hikari data source properties
	 */
	public Properties getHikari() {
		return this.hikari;
	}

	@Override
	protected String getUsername() {
		if (StringUtils.hasText(this.username)) {
			return this.username;
		}
		if (this.dataSourceClassName == null
				&& EmbeddedDatabaseConnection.isEmbedded(getDriverClassName())) {
			return "sa";
		}
		return null;
	}

}
