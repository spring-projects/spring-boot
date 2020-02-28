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

package org.springframework.boot.test.autoconfigure.data.r2dbc;

import io.r2dbc.spi.ConnectionFactory;
import org.junit.jupiter.api.Test;
import reactor.test.StepVerifier;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.data.r2dbc.connectionfactory.init.ResourceDatabasePopulator;
import org.springframework.data.r2dbc.core.DatabaseClient;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link DataR2dbcTest}.
 *
 * @author Mark Paluch
 */
@DataR2dbcTest
class DataR2dbcTestIntegrationTests {

	@Autowired
	private DatabaseClient databaseClient;

	@Autowired
	private ConnectionFactory connectionFactory;

	@Autowired
	private ApplicationContext applicationContext;

	@Test
	void testDatabaseClient() {
		this.databaseClient.execute("SELECT * FROM example").fetch().all().as(StepVerifier::create).verifyComplete();
	}

	@Test
	void replacesDefinedConnectionFactoryWithEmbeddedDefault() {
		String product = this.connectionFactory.getMetadata().getName();
		assertThat(product).isEqualTo("H2");
	}

	@Test
	void registersExampleRepository() {
		assertThat(this.applicationContext.getBeanNamesForType(ExampleRepository.class)).isNotEmpty();
	}

	@TestConfiguration
	static class DatabaseInitializationConfiguration {

		@Autowired
		void initializeDatabase(ConnectionFactory connectionFactory) {
			ResourceLoader resourceLoader = new DefaultResourceLoader();
			Resource[] scripts = new Resource[] { resourceLoader
					.getResource("classpath:org/springframework/boot/test/autoconfigure/data/r2dbc/schema.sql") };
			new ResourceDatabasePopulator(scripts).execute(connectionFactory).block();
		}

	}

}
