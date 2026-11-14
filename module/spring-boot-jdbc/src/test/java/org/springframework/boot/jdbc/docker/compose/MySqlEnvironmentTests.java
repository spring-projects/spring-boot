/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.jdbc.docker.compose;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * Tests for {@link MySqlEnvironment}.
 *
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 * @author Phillip Webb
 * @author Jinseong Hwang
 * @author Scott Frederick
 * @author MyungJoo Song
 */
class MySqlEnvironmentTests {

	@Test
	void createWhenHasMysqlRandomRootPasswordThrowsException() {
		assertThatIllegalStateException()
			.isThrownBy(() -> new MySqlEnvironment(Map.of("MYSQL_RANDOM_ROOT_PASSWORD", "true")))
			.withMessage("MYSQL_RANDOM_ROOT_PASSWORD is not supported without MYSQL_USER and MYSQL_PASSWORD");
	}

	@Test
	void createWhenHasMysqlRandomRootPasswordAndMysqlUserWithoutPasswordThrowsException() {
		assertThatIllegalStateException()
			.isThrownBy(() -> new MySqlEnvironment(
					Map.of("MYSQL_RANDOM_ROOT_PASSWORD", "true", "MYSQL_USER", "myself", "MYSQL_DATABASE", "db")))
			.withMessage("MYSQL_RANDOM_ROOT_PASSWORD is not supported without MYSQL_USER and MYSQL_PASSWORD");
	}

	@Test
	void getUsernameAndPasswordWhenHasMysqlRandomRootPasswordAndMysqlUserAndPassword() {
		MySqlEnvironment environment = new MySqlEnvironment(Map.of("MYSQL_RANDOM_ROOT_PASSWORD", "true", "MYSQL_USER",
				"myself", "MYSQL_PASSWORD", "secret", "MYSQL_DATABASE", "db"));
		assertThat(environment.getUsername()).isEqualTo("myself");
		assertThat(environment.getPassword()).isEqualTo("secret");
	}

	@Test
	void createWhenHasNoPasswordThrowsException() {
		assertThatIllegalStateException().isThrownBy(() -> new MySqlEnvironment(Collections.emptyMap()))
			.withMessage("No MySQL password found");
	}

	@Test
	void createWhenHasNoDatabaseThrowsException() {
		assertThatIllegalStateException().isThrownBy(() -> new MySqlEnvironment(Map.of("MYSQL_PASSWORD", "secret")))
			.withMessage("No MYSQL_DATABASE defined");
	}

	@Test
	void getUsernameWhenHasMysqlUser() {
		MySqlEnvironment environment = new MySqlEnvironment(
				Map.of("MYSQL_USER", "myself", "MYSQL_PASSWORD", "secret", "MYSQL_DATABASE", "db"));
		assertThat(environment.getUsername()).isEqualTo("myself");
	}

	@Test
	void getUsernameWhenHasNoMysqlUser() {
		MySqlEnvironment environment = new MySqlEnvironment(Map.of("MYSQL_PASSWORD", "secret", "MYSQL_DATABASE", "db"));
		assertThat(environment.getUsername()).isEqualTo("root");
	}

	@Test
	void getPasswordWhenHasMysqlPassword() {
		MySqlEnvironment environment = new MySqlEnvironment(Map.of("MYSQL_PASSWORD", "secret", "MYSQL_DATABASE", "db"));
		assertThat(environment.getPassword()).isEqualTo("secret");
	}

	@Test
	void getPasswordWhenHasMysqlRootPassword() {
		MySqlEnvironment environment = new MySqlEnvironment(
				Map.of("MYSQL_ROOT_PASSWORD", "secret", "MYSQL_DATABASE", "db"));
		assertThat(environment.getPassword()).isEqualTo("secret");
	}

	@Test
	void getPasswordWhenHasNoPasswordAndMysqlAllowEmptyPassword() {
		MySqlEnvironment environment = new MySqlEnvironment(
				Map.of("MYSQL_ALLOW_EMPTY_PASSWORD", "true", "MYSQL_DATABASE", "db"));
		assertThat(environment.getPassword()).isEmpty();
	}

	@Test
	void getPasswordWhenHasNoPasswordAndAllowEmptyPassword() {
		MySqlEnvironment environment = new MySqlEnvironment(
				Map.of("ALLOW_EMPTY_PASSWORD", "true", "MYSQL_DATABASE", "db"));
		assertThat(environment.getPassword()).isEmpty();
	}

	@Test
	void getDatabaseWhenHasMysqlDatabase() {
		MySqlEnvironment environment = new MySqlEnvironment(
				Map.of("MYSQL_ALLOW_EMPTY_PASSWORD", "true", "MYSQL_DATABASE", "db"));
		assertThat(environment.getDatabase()).isEqualTo("db");
	}

	@Test
	void createWhenHasMysqlPasswordFileThrowsException() {
		assertThatIllegalStateException()
			.isThrownBy(() -> new MySqlEnvironment(Map.of("MYSQL_USER", "myself", "MYSQL_PASSWORD_FILE",
					"/run/secrets/password", "MYSQL_DATABASE", "db")))
			.withMessage("MYSQL_PASSWORD_FILE is not supported");
	}

	@Test
	void createWhenHasMysqlRootPasswordFileThrowsException() {
		assertThatIllegalStateException()
			.isThrownBy(() -> new MySqlEnvironment(
					Map.of("MYSQL_ROOT_PASSWORD_FILE", "/run/secrets/password", "MYSQL_DATABASE", "db")))
			.withMessage("MYSQL_ROOT_PASSWORD_FILE is not supported");
	}

	@Test
	void createWhenHasMysqlUserFileThrowsException() {
		assertThatIllegalStateException()
			.isThrownBy(() -> new MySqlEnvironment(
					Map.of("MYSQL_USER_FILE", "/run/secrets/user", "MYSQL_PASSWORD", "secret", "MYSQL_DATABASE", "db")))
			.withMessage("MYSQL_USER_FILE is not supported");
	}

	@Test
	void createWhenHasMysqlDatabaseFileThrowsException() {
		assertThatIllegalStateException()
			.isThrownBy(() -> new MySqlEnvironment(
					Map.of("MYSQL_PASSWORD", "secret", "MYSQL_DATABASE_FILE", "/run/secrets/db")))
			.withMessage("MYSQL_DATABASE_FILE is not supported");
	}

	@ParameterizedTest
	@ValueSource(
			strings = { "MYSQL_USER_FILE", "MYSQL_PASSWORD_FILE", "MYSQL_ROOT_PASSWORD_FILE", "MYSQL_DATABASE_FILE" })
	void createWhenHasFileVariableAndPlainVariablesThrowsException(String fileVariable) {
		Map<String, @Nullable String> env = new HashMap<>(Map.of("MYSQL_USER", "myself", "MYSQL_PASSWORD", "secret",
				"MYSQL_ROOT_PASSWORD", "root", "MYSQL_DATABASE", "db"));
		env.put(fileVariable, "/run/secrets/value");
		assertThatIllegalStateException().isThrownBy(() -> new MySqlEnvironment(env))
			.withMessage(fileVariable + " is not supported");
	}

}
