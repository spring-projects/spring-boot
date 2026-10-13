/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.jdbc.docker.compose;

import java.util.Collections;
import java.util.Map;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * Tests for {@link SqlServerEnvironment}.
 *
 * @author Andy Wilkinson
 */
class SqlServerEnvironmentTests {

	@Test
	void createWhenHasNoPasswordThrowsException() {
		assertThatIllegalStateException().isThrownBy(() -> new SqlServerEnvironment(Collections.emptyMap()))
			.withMessage("No MSSQL password found");
	}

	@Test
	void getUsernameWhenHasNoMsSqlUser() {
		SqlServerEnvironment environment = new SqlServerEnvironment(Map.of("MSSQL_SA_PASSWORD", "secret"));
		assertThat(environment.getUsername()).isEqualTo("SA");
	}

	@Test
	void getPasswordWhenHasMsSqlSaPassword() {
		SqlServerEnvironment environment = new SqlServerEnvironment(Map.of("MSSQL_SA_PASSWORD", "secret"));
		assertThat(environment.getPassword()).isEqualTo("secret");
	}

	@Test
	void getPasswordWhenHasSaPassword() {
		SqlServerEnvironment environment = new SqlServerEnvironment(Map.of("SA_PASSWORD", "secret"));
		assertThat(environment.getPassword()).isEqualTo("secret");
	}

	@Test
	void getPasswordWhenHasMsSqlSaPasswordAndSaPasswordPrefersMsSqlSaPassword() {
		SqlServerEnvironment environment = new SqlServerEnvironment(
				Map.of("MSSQL_SA_PASSWORD", "secret", "SA_PASSWORD", "not used"));
		assertThat(environment.getPassword()).isEqualTo("secret");
	}

}
