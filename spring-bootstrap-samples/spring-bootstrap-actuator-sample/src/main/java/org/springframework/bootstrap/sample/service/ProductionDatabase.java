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
package org.springframework.bootstrap.sample.service;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * @author Dave Syer
 * 
 */
@Configuration
@Profile("prod")
public class ProductionDatabase {

	@Value("${spring.database.driverClassName:com.mysql.jdbc.Driver}")
	private String driverClassName;

	@Value("${spring.database.url:jdbc:mysql://localhost:3306/test}")
	private String url;

	@Value("${spring.database.username:root}")
	private String username;

	@Value("${spring.database.password:}")
	private String password;

	@Bean
	public DataSource dataSource() {
		org.apache.tomcat.jdbc.pool.DataSource pool = new org.apache.tomcat.jdbc.pool.DataSource();
		pool.setDriverClassName(this.driverClassName);
		pool.setUrl(this.url);
		pool.setUsername(this.username);
		pool.setPassword(this.password);
		return pool;
	}

}
