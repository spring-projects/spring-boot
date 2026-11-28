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
 * Tests for {@link ClickHouseEnvironment}.
 *
 * @author Stephane Nicoll
 * @author MyungJoo Song
 */
class ClickHouseEnvironmentTests {

	@Test
	void createWhenNoPasswordThrowsException() {
		assertThatIllegalStateException().isThrownBy(() -> new ClickHouseEnvironment(Collections.emptyMap()))
			.withMessage("No ClickHouse password found");
	}

	@Test
	void getPasswordWhenHasPassword() {
		ClickHouseEnvironment environment = new ClickHouseEnvironment(Map.of("CLICKHOUSE_PASSWORD", "secret"));
		assertThat(environment.getPassword()).isEqualTo("secret");
	}

	@Test
	void getPasswordWhenHasNoPasswordAndAllowEmptyPassword() {
		ClickHouseEnvironment environment = new ClickHouseEnvironment(Map.of("ALLOW_EMPTY_PASSWORD", "true"));
		assertThat(environment.getPassword()).isEmpty();
	}

	@Test
	void getPasswordWhenHasNoPasswordAndAllowEmptyPasswordIsYes() {
		ClickHouseEnvironment environment = new ClickHouseEnvironment(Map.of("ALLOW_EMPTY_PASSWORD", "yes"));
		assertThat(environment.getPassword()).isEmpty();
	}

	@Test
	void getUsernameWhenNoUser() {
		ClickHouseEnvironment environment = new ClickHouseEnvironment(Map.of("CLICKHOUSE_PASSWORD", "secret"));
		assertThat(environment.getUsername()).isEqualTo("default");
	}

	@Test
	void getUsernameWhenHasUser() {
		ClickHouseEnvironment environment = new ClickHouseEnvironment(
				Map.of("CLICKHOUSE_USER", "me", "CLICKHOUSE_PASSWORD", "secret"));
		assertThat(environment.getUsername()).isEqualTo("me");
	}

	@Test
	void getDatabaseWhenNoDatabase() {
		ClickHouseEnvironment environment = new ClickHouseEnvironment(Map.of("CLICKHOUSE_PASSWORD", "secret"));
		assertThat(environment.getDatabase()).isEqualTo("default");
	}

	@Test
	void getDatabaseWhenHasDatabase() {
		ClickHouseEnvironment environment = new ClickHouseEnvironment(
				Map.of("CLICKHOUSE_DB", "db", "CLICKHOUSE_PASSWORD", "secret"));
		assertThat(environment.getDatabase()).isEqualTo("db");
	}

	@Test
	void createWhenHasClickHousePasswordFileThrowsException() {
		assertThatIllegalStateException()
			.isThrownBy(() -> new ClickHouseEnvironment(Map.of("CLICKHOUSE_PASSWORD_FILE", "/run/secrets/password")))
			.withMessage("CLICKHOUSE_PASSWORD_FILE is not supported");
	}

	@ParameterizedTest
	@ValueSource(strings = { "CLICKHOUSE_PASSWORD_FILE" })
	void createWhenHasFileVariableAndPlainVariablesThrowsException(String fileVariable) {
		Map<String, @Nullable String> env = new HashMap<>(Map.of("CLICKHOUSE_PASSWORD", "secret"));
		env.put(fileVariable, "/run/secrets/value");
		assertThatIllegalStateException().isThrownBy(() -> new ClickHouseEnvironment(env))
			.withMessage(fileVariable + " is not supported");
	}

}
