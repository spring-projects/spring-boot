/*
 * Copyright 2012-present the original author or authors.
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
		helper = new CredentialHelper(getExecutableName());
	}

	private static String getExecutableName() throws Exception {
		if (Platform.isWindows()) {
			String executablePath = geExecutableAbsolutePath("docker-credential-test.bat");
			// cmd /c must resolve automatically .bat suffix
			return executablePath.substring(0, executablePath.lastIndexOf(".bat"));
		}
		return geExecutableAbsolutePath("docker-credential-test.sh");
	}

	private static String geExecutableAbsolutePath(String executableName) throws Exception {
		return new ClassPathResource(executableName, CredentialHelperTests.class).getFile().getAbsolutePath();
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
					assertThat(ex.getSuppressed()).satisfiesExactlyInAnyOrder(
							(suppressed) -> assertThat(suppressed)
								.hasMessageContaining("/opt/homebrew/bin/" + executable),
							(suppressed) -> assertThat(suppressed)
								.hasMessageContaining("/usr/local/bin/" + executable));
				}
			});
	}

}
