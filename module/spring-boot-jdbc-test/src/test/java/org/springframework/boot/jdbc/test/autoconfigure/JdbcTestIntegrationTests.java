/*
 * Copyright 2012-present the original author or authors.
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

package org.springframework.boot.jdbc.test.autoconfigure;

import java.util.Collection;

import javax.sql.DataSource;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.testcontainers.service.connection.ServiceConnectionAutoConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.springframework.boot.autoconfigure.AutoConfigurationImportedCondition.importedAutoConfiguration;

/**
 * Integration tests for {@link JdbcTest @JdbcTest}.
 *
 * @author Stephane Nicoll
 * @author Yanming Zhou
 */
@JdbcTest
@TestPropertySource(
		properties = "spring.sql.init.schemaLocations=classpath:org/springframework/boot/jdbc/test/autoconfigure/schema.sql")
class JdbcTestIntegrationTests {

	@Autowired
	private JdbcClient jdbcClient;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Autowired
	private DataSource dataSource;

	@Autowired
	private ApplicationContext applicationContext;

	@Test
	void testJdbcClient() {
		ExampleJdbcClientRepository repository = new ExampleJdbcClientRepository(this.jdbcClient);
		repository.save(new ExampleEntity(1, "John"));
		ExampleEntity entity = repository.findById(1);
		assertThat(entity.getId()).isOne();
		assertThat(entity.getName()).isEqualTo("John");
		Collection<ExampleEntity> entities = repository.findAll();
		assertThat(entities).hasSize(1);
		entity = entities.iterator().next();
		assertThat(entity.getId()).isOne();
		assertThat(entity.getName()).isEqualTo("John");
	}

	@Test
	void testJdbcTemplate() {
		ExampleRepository repository = new ExampleRepository(this.jdbcTemplate);
		repository.save(new ExampleEntity(1, "John"));
		ExampleEntity entity = repository.findById(1);
		assertThat(entity.getId()).isOne();
		assertThat(entity.getName()).isEqualTo("John");
		Collection<ExampleEntity> entities = repository.findAll();
		assertThat(entities).hasSize(1);
		entity = entities.iterator().next();
		assertThat(entity.getId()).isOne();
		assertThat(entity.getName()).isEqualTo("John");
	}

	@Test
	void replacesDefinedDataSourceWithEmbeddedDefault() throws Exception {
		String product = this.dataSource.getConnection().getMetaData().getDatabaseProductName();
		assertThat(product).isEqualTo("H2");
	}

	@Test
	void didNotInjectExampleRepository() {
		assertThatExceptionOfType(NoSuchBeanDefinitionException.class)
			.isThrownBy(() -> this.applicationContext.getBean(ExampleRepository.class));
	}

	@Test
	void serviceConnectionAutoConfigurationWasImported() {
		assertThat(this.applicationContext).has(importedAutoConfiguration(ServiceConnectionAutoConfiguration.class));
	}

}
