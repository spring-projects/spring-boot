/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.sql.init;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Arrays;

import org.junit.jupiter.api.Test;

import org.springframework.boot.testsupport.classpath.resources.WithResource;
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
	@WithSchemaSqlResource
	@WithDataSqlResource
	void whenDatabaseIsInitializedThenSchemaAndDataScriptsAreApplied() {
		DatabaseInitializationSettings settings = new DatabaseInitializationSettings();
		settings.setSchemaLocations(Arrays.asList("schema.sql"));
		settings.setDataLocations(Arrays.asList("data.sql"));
		T initializer = createEmbeddedDatabaseInitializer(settings);
		assertThat(initializer.initializeDatabase()).isTrue();
		assertThat(numberOfEmbeddedRows("SELECT COUNT(*) FROM EXAMPLE")).isOne();
	}

	@Test
	void whenDatabaseIsInitializedWithDirectoryLocationsThenFailureIsHelpful() {
		DatabaseInitializationSettings settings = new DatabaseInitializationSettings();
		settings.setSchemaLocations(Arrays.asList("/org/springframework/boot/sql/init"));
		settings.setDataLocations(Arrays.asList("/org/springframework/boot/sql/init"));
		T initializer = createEmbeddedDatabaseInitializer(settings);
		assertThatIllegalStateException().isThrownBy(initializer::initializeDatabase)
			.withMessage("No schema scripts found at location '/org/springframework/boot/sql/init'");
	}

	@Test
	@WithDataSqlResource
	void whenContinueOnErrorIsFalseThenInitializationFailsOnError() {
		DatabaseInitializationSettings settings = new DatabaseInitializationSettings();
		settings.setDataLocations(Arrays.asList("data.sql"));
		T initializer = createEmbeddedDatabaseInitializer(settings);
		assertThatExceptionOfType(DataAccessException.class).isThrownBy(initializer::initializeDatabase);
		assertThatDatabaseWasAccessed(initializer);
	}

	@Test
	@WithDataSqlResource
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
	@WithSchemaSqlResource
	@WithDataSqlResource
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
	@WithSchemaSqlResource
	@WithDataSqlResource
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
	@WithSchemaSqlResource
	@WithDataSqlResource
	void whenModeIsEmbeddedThenEmbeddedDatabaseIsInitialized() {
		DatabaseInitializationSettings settings = new DatabaseInitializationSettings();
		settings.setSchemaLocations(Arrays.asList("schema.sql"));
		settings.setDataLocations(Arrays.asList("data.sql"));
		settings.setMode(DatabaseInitializationMode.EMBEDDED);
		T initializer = createEmbeddedDatabaseInitializer(settings);
		assertThat(initializer.initializeDatabase()).isTrue();
		assertThat(numberOfEmbeddedRows("SELECT COUNT(*) FROM EXAMPLE")).isOne();
	}

	@Test
	@WithSchemaSqlResource
	@WithDataSqlResource
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
	@WithSchemaSqlResource
	@WithDataSqlResource
	void whenModeIsAlwaysThenEmbeddedDatabaseIsInitialized() {
		DatabaseInitializationSettings settings = new DatabaseInitializationSettings();
		settings.setSchemaLocations(Arrays.asList("schema.sql"));
		settings.setDataLocations(Arrays.asList("data.sql"));
		settings.setMode(DatabaseInitializationMode.ALWAYS);
		T initializer = createEmbeddedDatabaseInitializer(settings);
		assertThat(initializer.initializeDatabase()).isTrue();
		assertThat(numberOfEmbeddedRows("SELECT COUNT(*) FROM EXAMPLE")).isOne();
	}

	@Test
	@WithSchemaSqlResource
	@WithDataSqlResource
	void whenModeIsAlwaysThenStandaloneDatabaseIsInitialized() {
		DatabaseInitializationSettings settings = new DatabaseInitializationSettings();
		settings.setSchemaLocations(Arrays.asList("schema.sql"));
		settings.setDataLocations(Arrays.asList("data.sql"));
		settings.setMode(DatabaseInitializationMode.ALWAYS);
		T initializer = createStandaloneDatabaseInitializer(settings);
		assertThat(initializer.initializeDatabase()).isTrue();
		assertThat(numberOfStandaloneRows("SELECT COUNT(*) FROM EXAMPLE")).isOne();
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

	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.METHOD)
	@WithResource(name = "schema.sql", content = """
			CREATE TABLE EXAMPLE (
				id INTEGER GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
				name VARCHAR(30)
			);
			""")
	protected @interface WithSchemaSqlResource {

	}

	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.METHOD)
	@WithResource(name = "data.sql", content = "INSERT INTO EXAMPLE VALUES (1, 'Andy');")
	protected @interface WithDataSqlResource {

	}

}
