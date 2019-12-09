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
import org.springframework.boot.jdbc.EmbeddedDatabaseConnection;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.SimpleDriverDataSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link JdbcTest @JdbcTest}.
 *
 * @author Chris Bono
 */
@JdbcTest
@AutoConfigureTestDatabase(connection = EmbeddedDatabaseConnection.HSQL, replace = Replace.ANY,
		urlOptions = "DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE;MODE=MYSQL")
class JdbcTestWithAutoConfigureTestDatabaseAnnotationPropertiesIntegrationTests {

	@Autowired
	private DataSource dataSource;

	@Test
	void replacesDataSource() throws Exception {
		SimpleDriverDataSource actualDatasource = this.dataSource.unwrap(SimpleDriverDataSource.class);
		String url = actualDatasource.getUrl();
		assertThat(url).matches("jdbc:hsqldb:mem:[a-zA-Z0-9-]*;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE;MODE=MYSQL");
	}

	@Configuration(proxyBeanMethods = false)
	@EnableAutoConfiguration // Will auto-configure H2
	static class Config {

	}

}
