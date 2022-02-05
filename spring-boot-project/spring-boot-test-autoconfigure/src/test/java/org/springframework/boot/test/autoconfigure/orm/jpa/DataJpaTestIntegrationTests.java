/*
 * Copyright 2012-2022 the original author or authors.
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

package org.springframework.boot.test.autoconfigure.orm.jpa;

import javax.sql.DataSource;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration;
import org.springframework.boot.autoconfigure.liquibase.LiquibaseAutoConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.data.repository.config.BootstrapMode;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.springframework.boot.test.autoconfigure.AutoConfigurationImportedCondition.importedAutoConfiguration;

/**
 * Integration tests for {@link DataJpaTest @DataJpaTest}.
 *
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @author Scott Frederick
 */
@DataJpaTest
class DataJpaTestIntegrationTests {

	@Autowired
	private TestEntityManager entities;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Autowired
	private ExampleRepository repository;

	@Autowired
	private DataSource dataSource;

	@Autowired
	private ApplicationContext applicationContext;

	@Test
	void testEntityManager() {
		ExampleEntity entity = this.entities.persist(new ExampleEntity("spring", "123"));
		this.entities.flush();
		Object id = this.entities.getId(entity);
		ExampleEntity found = this.entities.find(ExampleEntity.class, id);
		assertThat(found.getName()).isEqualTo("spring");
	}

	@Test
	void testEntityManagerPersistAndGetId() {
		Long id = this.entities.persistAndGetId(new ExampleEntity("spring", "123"), Long.class);
		this.entities.flush();
		assertThat(id).isNotNull();
		String reference = this.jdbcTemplate.queryForObject("SELECT REFERENCE FROM EXAMPLE_ENTITY WHERE ID = ?",
				String.class, id);
		assertThat(reference).isEqualTo("123");
	}

	@Test
	void testRepository() {
		this.entities.persist(new ExampleEntity("spring", "123"));
		this.entities.persist(new ExampleEntity("boot", "124"));
		this.entities.flush();
		ExampleEntity found = this.repository.findByReference("124");
		assertThat(found.getName()).isEqualTo("boot");
	}

	@Test
	void replacesDefinedDataSourceWithEmbeddedDefault() throws Exception {
		String product = this.dataSource.getConnection().getMetaData().getDatabaseProductName();
		assertThat(product).isEqualTo("H2");
	}

	@Test
	void didNotInjectExampleComponent() {
		assertThatExceptionOfType(NoSuchBeanDefinitionException.class)
				.isThrownBy(() -> this.applicationContext.getBean(ExampleComponent.class));
	}

	@Test
	void flywayAutoConfigurationWasImported() {
		assertThat(this.applicationContext).has(importedAutoConfiguration(FlywayAutoConfiguration.class));
	}

	@Test
	void liquibaseAutoConfigurationWasImported() {
		assertThat(this.applicationContext).has(importedAutoConfiguration(LiquibaseAutoConfiguration.class));
	}

	@Test
	void bootstrapModeIsDefaultByDefault() {
		assertThat(this.applicationContext.getEnvironment().getProperty("spring.data.jpa.repositories.bootstrap-mode"))
				.isEqualTo(BootstrapMode.DEFAULT.name());
	}

}
