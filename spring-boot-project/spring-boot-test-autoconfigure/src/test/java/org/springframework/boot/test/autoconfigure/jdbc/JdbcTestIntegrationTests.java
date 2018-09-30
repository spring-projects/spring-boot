/*
 * Copyright 2012-2017 the original author or authors.
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

package org.springframework.boot.test.autoconfigure.jdbc;

import java.util.Collection;

import javax.sql.DataSource;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration;
import org.springframework.boot.autoconfigure.liquibase.LiquibaseAutoConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.test.autoconfigure.AutoConfigurationImportedCondition.importedAutoConfiguration;

/**
 * Integration tests for {@link JdbcTest}.
 *
 * @author Stephane Nicoll
 */
@RunWith(SpringRunner.class)
@JdbcTest
@TestPropertySource(properties = "spring.datasource.schema=classpath:org/springframework/boot/test/autoconfigure/jdbc/schema.sql")
public class JdbcTestIntegrationTests {

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Autowired
	private DataSource dataSource;

	@Autowired
	private ApplicationContext applicationContext;

	@Test
	public void testJdbcTemplate() {
		ExampleRepository repository = new ExampleRepository(this.jdbcTemplate);
		repository.save(new ExampleEntity(1, "John"));
		Collection<ExampleEntity> entities = repository.findAll();
		assertThat(entities).hasSize(1);
		ExampleEntity entity = entities.iterator().next();
		assertThat(entity.getId()).isEqualTo(1);
		assertThat(entity.getName()).isEqualTo("John");
	}

	@Test
	public void replacesDefinedDataSourceWithEmbeddedDefault() throws Exception {
		String product = this.dataSource.getConnection().getMetaData()
				.getDatabaseProductName();
		assertThat(product).isEqualTo("H2");
	}

	@Test
	public void didNotInjectExampleRepository() {
		this.thrown.expect(NoSuchBeanDefinitionException.class);
		this.applicationContext.getBean(ExampleRepository.class);
	}

	@Test
	public void flywayAutoConfigurationWasImported() {
		assertThat(this.applicationContext)
				.has(importedAutoConfiguration(FlywayAutoConfiguration.class));
	}

	@Test
	public void liquibaseAutoConfigurationWasImported() {
		assertThat(this.applicationContext)
				.has(importedAutoConfiguration(LiquibaseAutoConfiguration.class));
	}

}
