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
 * @param <T> type of the initializer being tested
 * @author Andy Wilkinson
 */
public abstract class AbstractScriptDatabaseInitializerTests<T extends AbstractScriptDatabaseInitializer> {

	@Test
	void whenDatabaseIsInitializedThenSchemaAndDataScriptsAreApplied() {
		DatabaseInitializationSettings settings = new DatabaseInitializationSettings();
		settings.setSchemaLocations(Arrays.asList("schema.sql"));
		settings.setDataLocations(Arrays.asList("data.sql"));
		T initializer = createEmbeddedDatabaseInitializer(settings);
		assertThat(initializer.initializeDatabase()).isTrue();
		assertThat(numberOfEmbeddedRows("SELECT COUNT(*) FROM EXAMPLE")).isEqualTo(1);
	}

	@Test
	void whenContinueOnErrorIsFalseThenInitializationFailsOnError() {
		DatabaseInitializationSettings settings = new DatabaseInitializationSettings();
		settings.setDataLocations(Arrays.asList("data.sql"));
		T initializer = createEmbeddedDatabaseInitializer(settings);
		assertThatExceptionOfType(DataAccessException.class).isThrownBy(() -> initializer.initializeDatabase());
		assertThatDatabaseWasAccessed(initializer);
	}

	@Test
	void whenContinueOnErrorIsTrueThenInitializationDoesNotFailOnError() {
		DatabaseInitializationSettings settings = new DatabaseInitializationSettings();
		settings.setContinueOnError(true);
		settings.setDataLocations(Arrays.asList("data.sql"));
		T initializer = createEmbeddedDatabaseInitializer(settings);
		assertThat(initializer.initializeDatabase()).isTrue();
		assertThatDatabaseWasAccessed(initializer);
	}

	@Test
	void whenNoScriptsExistAtASchemaLocationThenInitializationFails() {
		DatabaseInitializationSettings settings = new DatabaseInitializationSettings();
		settings.setSchemaLocations(Arrays.asList("does-not-exist.sql"));
		T initializer = createEmbeddedDatabaseInitializer(settings);
		assertThatIllegalStateException().isThrownBy(initializer::initializeDatabase)
				.withMessage("No schema scripts found at location 'does-not-exist.sql'");
		assertThatDatabaseWasNotAccessed(initializer);
	}

	@Test
	void whenNoScriptsExistAtADataLocationThenInitializationFails() {
		DatabaseInitializationSettings settings = new DatabaseInitializationSettings();
		settings.setDataLocations(Arrays.asList("does-not-exist.sql"));
		T initializer = createEmbeddedDatabaseInitializer(settings);
		assertThatIllegalStateException().isThrownBy(initializer::initializeDatabase)
				.withMessage("No data scripts found at location 'does-not-exist.sql'");
		assertThatDatabaseWasNotAccessed(initializer);
	}

	@Test
	void whenNoScriptsExistAtAnOptionalSchemaLocationThenDatabaseIsNotAccessed() {
		DatabaseInitializationSettings settings = new DatabaseInitializationSettings();
		settings.setSchemaLocations(Arrays.asList("optional:does-not-exist.sql"));
		T initializer = createEmbeddedDatabaseInitializer(settings);
		assertThat(initializer.initializeDatabase()).isFalse();
		assertThatDatabaseWasNotAccessed(initializer);
	}

	@Test
	void whenNoScriptsExistAtAnOptionalDataLocationThenDatabaseIsNotAccessed() {
		DatabaseInitializationSettings settings = new DatabaseInitializationSettings();
		settings.setDataLocations(Arrays.asList("optional:does-not-exist.sql"));
		T initializer = createEmbeddedDatabaseInitializer(settings);
		assertThat(initializer.initializeDatabase()).isFalse();
		assertThatDatabaseWasNotAccessed(initializer);
	}

	@Test
	void whenModeIsNeverThenEmbeddedDatabaseIsNotInitialized() {
		DatabaseInitializationSettings settings = new DatabaseInitializationSettings();
		settings.setSchemaLocations(Arrays.asList("schema.sql"));
		settings.setDataLocations(Arrays.asList("data.sql"));
		settings.setMode(DatabaseInitializationMode.NEVER);
		T initializer = createEmbeddedDatabaseInitializer(settings);
		assertThat(initializer.initializeDatabase()).isFalse();
		assertThatDatabaseWasNotAccessed(initializer);
	}

	@Test
	void whenModeIsNeverThenStandaloneDatabaseIsNotInitialized() {
		DatabaseInitializationSettings settings = new DatabaseInitializationSettings();
		settings.setSchemaLocations(Arrays.asList("schema.sql"));
		settings.setDataLocations(Arrays.asList("data.sql"));
		settings.setMode(DatabaseInitializationMode.NEVER);
		T initializer = createStandaloneDatabaseInitializer(settings);
		assertThat(initializer.initializeDatabase()).isFalse();
		assertThatDatabaseWasNotAccessed(initializer);
	}

	@Test
	void whenModeIsEmbeddedThenEmbeddedDatabaseIsInitialized() {
		DatabaseInitializationSettings settings = new DatabaseInitializationSettings();
		settings.setSchemaLocations(Arrays.asList("schema.sql"));
		settings.setDataLocations(Arrays.asList("data.sql"));
		settings.setMode(DatabaseInitializationMode.EMBEDDED);
		T initializer = createEmbeddedDatabaseInitializer(settings);
		assertThat(initializer.initializeDatabase()).isTrue();
		assertThat(numberOfEmbeddedRows("SELECT COUNT(*) FROM EXAMPLE")).isEqualTo(1);
	}

	@Test
	void whenModeIsEmbeddedThenStandaloneDatabaseIsNotInitialized() {
		DatabaseInitializationSettings settings = new DatabaseInitializationSettings();
		settings.setSchemaLocations(Arrays.asList("schema.sql"));
		settings.setDataLocations(Arrays.asList("data.sql"));
		settings.setMode(DatabaseInitializationMode.EMBEDDED);
		T initializer = createStandaloneDatabaseInitializer(settings);
		assertThat(initializer.initializeDatabase()).isFalse();
		assertThatDatabaseWasAccessed(initializer);
	}

	@Test
	void whenModeIsAlwaysThenEmbeddedDatabaseIsInitialized() {
		DatabaseInitializationSettings settings = new DatabaseInitializationSettings();
		settings.setSchemaLocations(Arrays.asList("schema.sql"));
		settings.setDataLocations(Arrays.asList("data.sql"));
		settings.setMode(DatabaseInitializationMode.ALWAYS);
		T initializer = createEmbeddedDatabaseInitializer(settings);
		assertThat(initializer.initializeDatabase()).isTrue();
		assertThat(numberOfEmbeddedRows("SELECT COUNT(*) FROM EXAMPLE")).isEqualTo(1);
	}

	@Test
	void whenModeIsAlwaysThenStandaloneDatabaseIsInitialized() {
		DatabaseInitializationSettings settings = new DatabaseInitializationSettings();
		settings.setSchemaLocations(Arrays.asList("schema.sql"));
		settings.setDataLocations(Arrays.asList("data.sql"));
		settings.setMode(DatabaseInitializationMode.ALWAYS);
		T initializer = createStandaloneDatabaseInitializer(settings);
		assertThat(initializer.initializeDatabase()).isTrue();
		assertThat(numberOfStandaloneRows("SELECT COUNT(*) FROM EXAMPLE")).isEqualTo(1);
	}

	protected abstract T createStandaloneDatabaseInitializer(DatabaseInitializationSettings settings);

	protected abstract T createEmbeddedDatabaseInitializer(DatabaseInitializationSettings settings);

	protected abstract int numberOfEmbeddedRows(String sql);

	protected abstract int numberOfStandaloneRows(String sql);

	private void assertThatDatabaseWasAccessed(T initializer) {
		assertDatabaseAccessed(true, initializer);
	}

	private void assertThatDatabaseWasNotAccessed(T initializer) {
		assertDatabaseAccessed(false, initializer);
	}

	protected abstract void assertDatabaseAccessed(boolean accessed, T initializer);

}
