/*
 * Copyright 2012-2025 the original author or authors.
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

package org.springframework.boot.buildpack.platform.docker.configuration;

import java.io.IOException;
import java.util.UUID;

import com.sun.jna.Platform;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.core.io.ClassPathResource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIOException;

/**
 * Tests for {@link DefaultDockerCredentialHelper}.
 *
 * @author Dmytro Nosan
 */
class DefaultDockerCredentialHelperTests {

	private DefaultDockerCredentialHelper helper;

	@BeforeEach
	void setUp() throws IOException {
		String name = "docker-credential-test";
		if (Platform.isWindows()) {
			name += ".bat";
		}
		this.helper = new DefaultDockerCredentialHelper(
				new ClassPathResource(name, getClass()).getFile().getAbsolutePath());
	}

	@Test
	void shouldReturnCredentialsForUser() throws IOException {
		Credentials credentials = this.helper.get("user.example.com");
		assertThat(credentials).isNotNull();
		assertThat(credentials.isIdentityToken()).isFalse();
		assertThat(credentials.getServerUrl()).isEqualTo("user.example.com");
		assertThat(credentials.getUsername()).isEqualTo("username");
		assertThat(credentials.getSecret()).isEqualTo("secret");
	}

	@Test
	void shouldReturnCredentialsForToken() throws IOException {
		Credentials credentials = this.helper.get("token.example.com");
		assertThat(credentials).isNotNull();
		assertThat(credentials.isIdentityToken()).isTrue();
		assertThat(credentials.getServerUrl()).isEqualTo("token.example.com");
		assertThat(credentials.getUsername()).isEqualTo("<token>");
		assertThat(credentials.getSecret()).isEqualTo("secret");
	}

	@Test
	void shouldReturnNullCredentialsWhenCredentialsNotFoundError() throws IOException {
		Credentials credentials = this.helper.get("credentials.missing.example.com");
		assertThat(credentials).isNull();
	}

	@Test
	void shouldReturnNullCredentialsWhenUsernameMissingError() throws IOException {
		Credentials credentials = this.helper.get("username.missing.example.com");
		assertThat(credentials).isNull();
	}

	@Test
	void shouldReturnNullCredentialsWhenServerUrlMissingError() throws IOException {
		Credentials credentials = this.helper.get("url.missing.example.com");
		assertThat(credentials).isNull();
	}

	@Test
	void shouldThrowIOExceptionWhenUnknownError() {
		assertThatIOException().isThrownBy(() -> this.helper.get("invalid.example.com"))
			.withMessageContaining("Unknown error");
	}

	@Test
	void shouldThrowIOExceptionWhenCommandDoesNotExist() {
		String name = "docker-credential-%s".formatted(UUID.randomUUID().toString());
		assertThatIOException().isThrownBy(() -> new DefaultDockerCredentialHelper(name).get("invalid.example.com"))
			.withMessageContaining(name);
	}

}
