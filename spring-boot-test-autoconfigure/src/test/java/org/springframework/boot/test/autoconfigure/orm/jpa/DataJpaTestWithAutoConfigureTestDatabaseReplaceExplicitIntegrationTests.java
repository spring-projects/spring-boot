/*
 * Copyright 2012-2016 the original author or authors.
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

package org.springframework.boot.test.autoconfigure.orm.jpa;

import javax.sql.DataSource;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.EmbeddedDatabaseConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
import org.springframework.test.context.junit4.SpringRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link DataJpaTest}.
 *
 * @author Phillip Webb
 */
@RunWith(SpringRunner.class)
@DataJpaTest
@AutoConfigureTestDatabase(connection = EmbeddedDatabaseConnection.HSQL)
public class DataJpaTestWithAutoConfigureTestDatabaseReplaceExplicitIntegrationTests {

	@Autowired
	private TestEntityManager entities;

	@Autowired
	private ExampleRepository repository;

	@Autowired
	private DataSource dataSource;

	@Test
	public void testRepository() throws Exception {
		this.entities.persist(new ExampleEntity("spring", "123"));
		this.entities.persist(new ExampleEntity("boot", "124"));
		this.entities.flush();
		ExampleEntity found = this.repository.findByReference("124");
		assertThat(found.getName()).isEqualTo("boot");
	}

	@Test
	public void replacesDefinedDataSourceWithExplicit() throws Exception {
		// H2 is explicitly defined but HSQL is the override.
		String product = this.dataSource.getConnection().getMetaData()
				.getDatabaseProductName();
		assertThat(product).startsWith("HSQL");
	}

	@Configuration
	@EnableAutoConfiguration
	static class Config {

		@Bean
		public DataSource dataSource() {
			EmbeddedDatabaseBuilder builder = new EmbeddedDatabaseBuilder()
					.setType(EmbeddedDatabaseType.H2);
			return builder.build();
		}

	}

}
