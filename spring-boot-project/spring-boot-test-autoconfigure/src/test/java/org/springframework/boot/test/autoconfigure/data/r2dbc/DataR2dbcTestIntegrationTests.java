/*
 * Copyright 2012-2023 the original author or authors.
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

import java.time.Duration;
import java.util.Map;

import io.r2dbc.spi.ConnectionFactory;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.testcontainers.service.connection.ServiceConnectionAutoConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.r2dbc.core.DatabaseClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.test.autoconfigure.AutoConfigurationImportedCondition.importedAutoConfiguration;

/**
 * Integration tests for {@link DataR2dbcTest}.
 *
 * @author Mark Paluch
 */
@DataR2dbcTest(
		properties = "spring.sql.init.schemaLocations=classpath:org/springframework/boot/test/autoconfigure/data/r2dbc/schema.sql")
class DataR2dbcTestIntegrationTests {

	@Autowired
	private DatabaseClient databaseClient;

	@Autowired
	private ConnectionFactory connectionFactory;

	@Autowired
	private ApplicationContext applicationContext;

	@Test
	void testDatabaseClient() {
		Flux<Map<String, Object>> all = this.databaseClient.sql("SELECT * FROM example").fetch().all();
		StepVerifier.create(all).expectNextCount(1).expectComplete().verify(Duration.ofSeconds(30));
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

	@Test
	void serviceConnectionAutoConfigurationWasImported() {
		assertThat(this.applicationContext).has(importedAutoConfiguration(ServiceConnectionAutoConfiguration.class));
	}

}
