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

package org.springframework.boot.autoconfigure.session;

import javax.sql.DataSource;

import org.junit.jupiter.api.Test;

import org.springframework.boot.sql.init.DatabaseInitializationMode;
import org.springframework.boot.sql.init.DatabaseInitializationSettings;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link JdbcSessionDataSourceScriptDatabaseInitializer}.
 *
 * @author Stephane Nicoll
 */
class JdbcSessionDataSourceScriptDatabaseInitializerTests {

	@Test
	void getSettingsWithPlatformDoesNotTouchDataSource() {
		DataSource dataSource = mock(DataSource.class);
		JdbcSessionProperties properties = new JdbcSessionProperties();
		properties.setPlatform("test");
		DatabaseInitializationSettings settings = JdbcSessionDataSourceScriptDatabaseInitializer.getSettings(dataSource,
				properties);
		assertThat(settings.getSchemaLocations())
				.containsOnly("classpath:org/springframework/session/jdbc/schema-test.sql");
		then(dataSource).shouldHaveNoInteractions();
	}

	@Test
	void test_validateConfigurationThrowsForInvalidConfiguration() {
		DataSource dataSource = mock(DataSource.class);
		JdbcSessionProperties properties = new JdbcSessionProperties();
		properties.setPlatform("mysql");
		JdbcSessionDataSourceScriptDatabaseInitializer initializer =
				new JdbcSessionDataSourceScriptDatabaseInitializer(dataSource, properties) {
					@Override
					protected boolean isEmbeddedDatabase() {
						return false;
					}
				};

		assertThatCode(initializer::validateConfiguration).doesNotThrowAnyException();

		properties.setInitializeSchema(DatabaseInitializationMode.ALWAYS);
		assertThatCode(initializer::validateConfiguration).doesNotThrowAnyException();

		properties.setTableName(properties.getTableName() + "_CUSTOM");
		assertThatThrownBy(initializer::validateConfiguration).message().isNotBlank();

		properties.setSchema(properties.getSchema() + ".different.sql");
		assertThatCode(initializer::validateConfiguration).doesNotThrowAnyException();
	}

}
