/*
 * Copyright 2012-2013 the original author or authors.
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
package org.springframework.bootstrap.autoconfigure.jdbc;

import javax.sql.DataSource;

import org.apache.commons.dbcp.BasicDataSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for a Commons DBCP database pool. The DBCP pool is popular but not
 * recommended in high volume environments.
 * 
 * @author Dave Syer
 * 
 */
@Configuration
public class BasicDataSourceConfiguration extends AbstractDataSourceConfiguration {

	@Bean
	public DataSource dataSource() {
		BasicDataSource pool = new BasicDataSource();
		pool.setDriverClassName(getDriverClassName());
		pool.setUrl(getUrl());
		pool.setUsername(getUsername());
		pool.setPassword(getPassword());
		return pool;
	}

}
