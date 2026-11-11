/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.mongodb.docker.compose;

import java.util.Collections;
import java.util.Map;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * Tests for {@link MongoEnvironment}.
 *
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 * @author Phillip Webb
 */
class MongoEnvironmentTests {

	@Test
	void createWhenMonoInitdbRootUsernameFileSetThrowsException() {
		assertThatIllegalStateException()
			.isThrownBy(() -> new MongoEnvironment(Map.of("MONGO_INITDB_ROOT_USERNAME_FILE", "file")))
			.withMessage("MONGO_INITDB_ROOT_USERNAME_FILE is not supported");
	}

	@Test
	void createWhenMonoInitdbRootPasswordFileSetThrowsException() {
		assertThatIllegalStateException()
			.isThrownBy(() -> new MongoEnvironment(Map.of("MONGO_INITDB_ROOT_PASSWORD_FILE", "file")))
			.withMessage("MONGO_INITDB_ROOT_PASSWORD_FILE is not supported");
	}

	@Test
	void getUsernameWhenHasNoMongoInitdbRootUsernameSet() {
		MongoEnvironment environment = new MongoEnvironment(Collections.emptyMap());
		assertThat(environment.getUsername()).isNull();
	}

	@Test
	void getUsernameWhenHasMongoInitdbRootUsernameSet() {
		MongoEnvironment environment = new MongoEnvironment(Map.of("MONGO_INITDB_ROOT_USERNAME", "user"));
		assertThat(environment.getUsername()).isEqualTo("user");
	}

	@Test
	void getPasswordWhenHasNoMongoInitdbRootPasswordSet() {
		MongoEnvironment environment = new MongoEnvironment(Collections.emptyMap());
		assertThat(environment.getPassword()).isNull();
	}

	@Test
	void getPasswordWhenHasMongoInitdbRootPasswordSet() {
		MongoEnvironment environment = new MongoEnvironment(Map.of("MONGO_INITDB_ROOT_PASSWORD", "secret"));
		assertThat(environment.getPassword()).isEqualTo("secret");
	}

	@Test
	void getDatabaseWhenHasNoMongoInitdbDatabaseSet() {
		MongoEnvironment environment = new MongoEnvironment(Collections.emptyMap());
		assertThat(environment.getDatabase()).isNull();
	}

	@Test
	void getDatabaseWhenHasMongoInitdbDatabaseSet() {
		MongoEnvironment environment = new MongoEnvironment(Map.of("MONGO_INITDB_DATABASE", "db"));
		assertThat(environment.getDatabase()).isEqualTo("db");
	}

}
