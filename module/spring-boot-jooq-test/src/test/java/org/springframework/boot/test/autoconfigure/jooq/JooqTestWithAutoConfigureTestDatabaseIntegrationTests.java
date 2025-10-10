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

package org.springframework.boot.test.autoconfigure.jooq;

import javax.sql.DataSource;

import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.EmbeddedDatabaseConnection;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jooq.test.autoconfigure.JooqTest;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link JooqTest @JooqTest}.
 *
 * @author Stephane Nicoll
 */
@JooqTest
@AutoConfigureTestDatabase(connection = EmbeddedDatabaseConnection.H2)
@TestPropertySource(properties = { "spring.flyway.enabled=false", "spring.liquibase.enabled=false" })
class JooqTestWithAutoConfigureTestDatabaseIntegrationTests {

	@Autowired
	private DSLContext dsl;

	@Autowired
	private DataSource dataSource;

	@Test
	void replacesAutoConfiguredDataSource() throws Exception {
		String product = this.dataSource.getConnection().getMetaData().getDatabaseProductName();
		assertThat(product).startsWith("H2");
		assertThat(this.dsl.configuration().dialect()).isEqualTo(SQLDialect.H2);
	}

}
