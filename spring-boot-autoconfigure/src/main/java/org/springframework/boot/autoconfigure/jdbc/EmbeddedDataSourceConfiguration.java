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

package org.springframework.boot.autoconfigure.jdbc;

import javax.annotation.PreDestroy;
import javax.sql.DataSource;

import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabase;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;

/**
 * Configuration for embedded data sources.
 *
 * @author Phillip Webb
 * @see DataSourceAutoConfiguration
 */
@Configuration
public class EmbeddedDataSourceConfiguration implements BeanClassLoaderAware {

	private EmbeddedDatabase database;

	private ClassLoader classLoader;

	@Value("${spring.datasource.name:testdb}")
	private String name = "testdb";

	@Override
	public void setBeanClassLoader(ClassLoader classLoader) {
		this.classLoader = classLoader;
	}

	@Bean
	public DataSource dataSource() {
		EmbeddedDatabaseBuilder builder = new EmbeddedDatabaseBuilder()
				.setType(EmbeddedDatabaseConnection.get(this.classLoader).getType());
		this.database = builder.setName(this.name).build();
		return this.database;
	}

	@PreDestroy
	public void close() {
		if (this.database != null) {
			this.database.shutdown();
		}
	}

}
