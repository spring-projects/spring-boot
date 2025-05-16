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

import java.util.UUID;

import com.sun.jna.Platform;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import org.springframework.core.io.ClassPathResource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIOException;

/**
 * Tests for {@link CredentialHelper}.
 *
 * @author Dmytro Nosan
 */
class CredentialHelperTests {

	private static CredentialHelper helper;

	@BeforeAll
	static void setUp() throws Exception {
		String executableName = "docker-credential-test" + ((Platform.isWindows()) ? ".bat" : ".sh");
		String executable = new ClassPathResource(executableName, CredentialHelperTests.class).getFile()
			.getAbsolutePath();
		helper = new CredentialHelper(executable);
	}

	@Test
	void getWhenKnowUser() throws Exception {
		Credential credentials = helper.get("user.example.com");
		assertThat(credentials).isNotNull();
		assertThat(credentials.isIdentityToken()).isFalse();
		assertThat(credentials.getServerUrl()).isEqualTo("user.example.com");
		assertThat(credentials.getUsername()).isEqualTo("username");
		assertThat(credentials.getSecret()).isEqualTo("secret");
	}

	@Test
	void getWhenKnowToken() throws Exception {
		Credential credentials = helper.get("token.example.com");
		assertThat(credentials).isNotNull();
		assertThat(credentials.isIdentityToken()).isTrue();
		assertThat(credentials.getServerUrl()).isEqualTo("token.example.com");
		assertThat(credentials.getUsername()).isEqualTo("<token>");
		assertThat(credentials.getSecret()).isEqualTo("secret");
	}

	@Test
	void getWhenCredentialsMissingMessageReturnsNull() throws Exception {
		Credential credentials = helper.get("credentials.missing.example.com");
		assertThat(credentials).isNull();
	}

	@Test
	void getWhenUsernameMissingMessageReturnsNull() throws Exception {
		Credential credentials = helper.get("username.missing.example.com");
		assertThat(credentials).isNull();
	}

	@Test
	void getWhenUrlMissingMessageReturnsNull() throws Exception {
		Credential credentials = helper.get("url.missing.example.com");
		assertThat(credentials).isNull();
	}

	@Test
	void getWhenUnknownErrorThrowsException() {
		assertThatIOException().isThrownBy(() -> helper.get("invalid.example.com"))
			.withMessageContaining("Unknown error");
	}

	@Test
	void getWhenExecutableDoesNotExistErrorThrowsException() {
		String executable = "docker-credential-%s".formatted(UUID.randomUUID().toString());
		assertThatIOException().isThrownBy(() -> new CredentialHelper(executable).get("invalid.example.com"))
			.withMessageContaining(executable)
			.satisfies((ex) -> {
				if (Platform.isMac()) {
					assertThat(ex.getMessage()).doesNotContain("/usr/local/bin/");
					assertThat(ex.getSuppressed()).allSatisfy((suppressed) -> assertThat(suppressed)
						.hasMessageContaining("/usr/local/bin/" + executable));
				}
			});
	}

}
