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

package org.springframework.boot.test.autoconfigure.orm.jpa;

import javax.sql.DataSource;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link DataJpaTest @DataJpaTest} with schema credentials that
 * should be ignored to allow the auto-configured test database to be used.
 *
 * @author Andy Wilkinson
 */
@DataJpaTest(properties = { "spring.sql.init.username=alice", "spring.sql.init.password=secret",
		"spring.sql.init.schema-locations=classpath:org/springframework/boot/test/autoconfigure/orm/jpa/schema.sql" })
class DataJpaTestSchemaCredentialsIntegrationTests {

	@Autowired
	private DataSource dataSource;

	@Test
	void replacesDefinedDataSourceWithEmbeddedDefault() throws Exception {
		String product = this.dataSource.getConnection().getMetaData().getDatabaseProductName();
		assertThat(product).isEqualTo("H2");
		assertThat(new JdbcTemplate(this.dataSource).queryForList("SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES",
				String.class))
			.contains("EXAMPLE");
	}

}
