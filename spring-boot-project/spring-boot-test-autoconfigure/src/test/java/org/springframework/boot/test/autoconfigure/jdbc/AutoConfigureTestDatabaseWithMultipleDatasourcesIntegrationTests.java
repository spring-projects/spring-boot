/*
 * Copyright 2012-2019 the original author or authors.
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

package org.springframework.boot.test.autoconfigure.jdbc;

import javax.sql.DataSource;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link AutoConfigureTestDatabase @AutoConfigureTestDatabase} when
 * there are multiple datasources.
 *
 * @author Greg Potter
 */
@JdbcTest
@AutoConfigureTestDatabase
class AutoConfigureTestDatabaseWithMultipleDatasourcesIntegrationTests {

	@Autowired
	private DataSource dataSource;

	@Test
	void replacesDefinedDataSourceWithExplicit() throws Exception {
		// Look that the datasource is replaced with an H2 DB.
		String product = this.dataSource.getConnection().getMetaData().getDatabaseProductName();
		assertThat(product).startsWith("H2");
	}

	@Configuration(proxyBeanMethods = false)
	@EnableAutoConfiguration
	static class Config {

		@Bean
		@Primary
		DataSource dataSource() {
			return new EmbeddedDatabaseBuilder().generateUniqueName(true).setType(EmbeddedDatabaseType.HSQL).build();
		}

		@Bean
		DataSource secondaryDataSource() {
			return new EmbeddedDatabaseBuilder().generateUniqueName(true).setType(EmbeddedDatabaseType.HSQL).build();
		}

	}

}
