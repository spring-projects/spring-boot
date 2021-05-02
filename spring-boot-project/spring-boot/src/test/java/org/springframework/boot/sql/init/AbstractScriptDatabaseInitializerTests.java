/*
 * Copyright 2012-2021 the original author or authors.
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

package org.springframework.boot.sql.init;

import java.util.Arrays;

import org.junit.jupiter.api.Test;

import org.springframework.dao.DataAccessException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * Base class for testing {@link AbstractScriptDatabaseInitializer} implementations.
 *
 * @author Andy Wilkinson
 */
public abstract class AbstractScriptDatabaseInitializerTests {

	@Test
	void whenDatabaseIsInitializedThenSchemaAndDataScriptsAreApplied() {
		DatabaseInitializationSettings settings = new DatabaseInitializationSettings();
		settings.setSchemaLocations(Arrays.asList("schema.sql"));
		settings.setDataLocations(Arrays.asList("data.sql"));
		AbstractScriptDatabaseInitializer initializer = createInitializer(settings);
		assertThat(initializer.initializeDatabase()).isTrue();
		assertThat(numberOfRows("SELECT COUNT(*) FROM EXAMPLE")).isEqualTo(1);
	}

	@Test
	void whenContinueOnErrorIsFalseThenInitializationFailsOnError() {
		DatabaseInitializationSettings settings = new DatabaseInitializationSettings();
		settings.setDataLocations(Arrays.asList("data.sql"));
		AbstractScriptDatabaseInitializer initializer = createInitializer(settings);
		assertThatExceptionOfType(DataAccessException.class).isThrownBy(() -> initializer.initializeDatabase());
	}

	@Test
	void whenContinueOnErrorIsTrueThenInitializationDoesNotFailOnError() {
		DatabaseInitializationSettings settings = new DatabaseInitializationSettings();
		settings.setContinueOnError(true);
		settings.setDataLocations(Arrays.asList("data.sql"));
		AbstractScriptDatabaseInitializer initializer = createInitializer(settings);
		assertThat(initializer.initializeDatabase()).isTrue();
	}

	@Test
	void whenNoScriptsExistAtASchemaLocationThenInitializationFails() {
		DatabaseInitializationSettings settings = new DatabaseInitializationSettings();
		settings.setSchemaLocations(Arrays.asList("does-not-exist.sql"));
		AbstractScriptDatabaseInitializer initializer = createInitializer(settings);
		assertThatIllegalStateException().isThrownBy(initializer::initializeDatabase)
				.withMessage("No schema scripts found at location 'does-not-exist.sql'");
	}

	@Test
	void whenNoScriptsExistAtADataLocationThenInitializationFails() {
		DatabaseInitializationSettings settings = new DatabaseInitializationSettings();
		settings.setDataLocations(Arrays.asList("does-not-exist.sql"));
		AbstractScriptDatabaseInitializer initializer = createInitializer(settings);
		assertThatIllegalStateException().isThrownBy(initializer::initializeDatabase)
				.withMessage("No data scripts found at location 'does-not-exist.sql'");
	}

	@Test
	void whenNoScriptsExistAtAnOptionalSchemaLocationThenInitializationSucceeds() {
		DatabaseInitializationSettings settings = new DatabaseInitializationSettings();
		settings.setSchemaLocations(Arrays.asList("optional:does-not-exist.sql"));
		AbstractScriptDatabaseInitializer initializer = createInitializer(settings);
		assertThat(initializer.initializeDatabase()).isFalse();
	}

	@Test
	void whenNoScriptsExistAtAnOptionalDataLocationThenInitializationSucceeds() {
		DatabaseInitializationSettings settings = new DatabaseInitializationSettings();
		settings.setDataLocations(Arrays.asList("optional:does-not-exist.sql"));
		AbstractScriptDatabaseInitializer initializer = createInitializer(settings);
		assertThat(initializer.initializeDatabase()).isFalse();
	}

	protected abstract AbstractScriptDatabaseInitializer createInitializer(DatabaseInitializationSettings settings);

	protected abstract int numberOfRows(String sql);

}
