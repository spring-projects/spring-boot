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

package org.springframework.boot.autoconfigure.flyway;

import org.flywaydb.core.api.configuration.FluentConfiguration;
import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.flyway.FlywayFactory.OracleFlywayConfigurationCustomizer;
import org.springframework.boot.autoconfigure.flyway.FlywayFactory.PostgresqlFlywayConfigurationCustomizer;
import org.springframework.boot.autoconfigure.flyway.FlywayFactory.SqlServerFlywayConfigurationCustomizer;

import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

public class FlywayFactoryTests {

	@Test
	void oracleExtensionIsNotLoadedByDefault() {
		FluentConfiguration configuration = mock(FluentConfiguration.class);
		new OracleFlywayConfigurationCustomizer(new FlywayProperties()).customize(configuration);
		then(configuration).shouldHaveNoInteractions();
	}

	@Test
	void sqlServerExtensionIsNotLoadedByDefault() {
		FluentConfiguration configuration = mock(FluentConfiguration.class);
		new SqlServerFlywayConfigurationCustomizer(new FlywayProperties()).customize(configuration);
		then(configuration).shouldHaveNoInteractions();
	}

	@Test
	void postgresqlExtensionIsNotLoadedByDefault() {
		FluentConfiguration configuration = mock(FluentConfiguration.class);
		new PostgresqlFlywayConfigurationCustomizer(new FlywayProperties()).customize(configuration);
		then(configuration).shouldHaveNoInteractions();
	}

}
