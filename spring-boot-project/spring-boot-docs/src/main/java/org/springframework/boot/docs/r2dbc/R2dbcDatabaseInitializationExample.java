/*
 * Copyright 2012-2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.docs.r2dbc;

import io.r2dbc.spi.ConnectionFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.r2dbc.connection.init.ResourceDatabasePopulator;

/**
 * Example configuration for initializing a database using R2DBC.
 *
 * @author Stephane Nicoll
 */
public class R2dbcDatabaseInitializationExample {

	// tag::configuration[]
	@Configuration(proxyBeanMethods = false)
	static class DatabaseInitializationConfiguration {

		@Autowired
		void initializeDatabase(ConnectionFactory connectionFactory) {
			ResourceLoader resourceLoader = new DefaultResourceLoader();
			Resource[] scripts = new Resource[] { resourceLoader.getResource("classpath:schema.sql"),
					resourceLoader.getResource("classpath:data.sql") };
			new ResourceDatabasePopulator(scripts).populate(connectionFactory).block();
		}

	}
	// end::configuration[]

}
