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

package org.springframework.boot.jdbc.autoconfigure;

import org.apache.commons.dbcp2.BasicDataSource;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.jdbc.DatabaseDriver;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link Dbcp2JdbcConnectionDetailsBeanPostProcessor}.
 *
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 * @author Phillip Webb
 */
class Dbcp2JdbcConnectionDetailsBeanPostProcessorTests {

	@Test
	@SuppressWarnings("unchecked")
	void setUsernamePasswordUrlAndDriverClassName() {
		BasicDataSource dataSource = new BasicDataSource();
		dataSource.setUrl("will-be-overwritten");
		dataSource.setUsername("will-be-overwritten");
		dataSource.setPassword("will-be-overwritten");
		dataSource.setDriverClassName("will-be-overwritten");
		new Dbcp2JdbcConnectionDetailsBeanPostProcessor(mock(ObjectProvider.class)).processDataSource(dataSource,
				new TestJdbcConnectionDetails());
		assertThat(dataSource.getUrl()).isEqualTo("jdbc:customdb://customdb.example.com:12345/database-1");
		assertThat(dataSource.getUserName()).isEqualTo("user-1");
		assertThat(dataSource).extracting("password").isEqualTo("password-1");
		assertThat(dataSource.getDriverClassName()).isEqualTo(DatabaseDriver.POSTGRESQL.getDriverClassName());
	}

}
