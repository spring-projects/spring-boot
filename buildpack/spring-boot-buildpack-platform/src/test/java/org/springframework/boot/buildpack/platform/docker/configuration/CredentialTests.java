/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.buildpack.platform.docker.configuration;

import java.io.IOException;
import java.io.InputStream;

import org.junit.jupiter.api.Test;

import org.springframework.boot.buildpack.platform.json.SharedJsonMapper;
import org.springframework.boot.testsupport.classpath.resources.WithResource;
import org.springframework.core.io.ClassPathResource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link Credential}.
 *
 * @author Dmytro Nosan
 */
class CredentialTests {

	@Test
	@WithResource(name = "credentials.json", content = """
			{
			  "ServerURL": "https://index.docker.io/v1/",
			  "Username": "user",
			  "Secret": "secret"
			}
			""")
	void createWhenUserCredentials() throws Exception {
		Credential credentials = getCredentials("credentials.json");
		assertThat(credentials.getUsername()).isEqualTo("user");
		assertThat(credentials.getSecret()).isEqualTo("secret");
		assertThat(credentials.getServerUrl()).isEqualTo("https://index.docker.io/v1/");
		assertThat(credentials.isIdentityToken()).isFalse();
	}

	@Test
	@WithResource(name = "credentials.json", content = """
			{
			  "ServerURL": "https://index.docker.io/v1/",
			  "Username": "<token>",
			  "Secret": "secret"
			}
			""")
	void createWhenTokenCredentials() throws Exception {
		Credential credentials = getCredentials("credentials.json");
		assertThat(credentials.getUsername()).isEqualTo("<token>");
		assertThat(credentials.getSecret()).isEqualTo("secret");
		assertThat(credentials.getServerUrl()).isEqualTo("https://index.docker.io/v1/");
		assertThat(credentials.isIdentityToken()).isTrue();
	}

	@Test
	@WithResource(name = "credentials.json", content = """
			{
			  "Username": "user",
			  "Secret": "secret"
			}
			""")
	void createWhenNoServerUrl() throws Exception {
		Credential credentials = getCredentials("credentials.json");
		assertThat(credentials.getUsername()).isEqualTo("user");
		assertThat(credentials.getSecret()).isEqualTo("secret");
		assertThat(credentials.getServerUrl()).isNull();
		assertThat(credentials.isIdentityToken()).isFalse();
	}

	private Credential getCredentials(String name) throws IOException {
		try (InputStream inputStream = new ClassPathResource(name).getInputStream()) {
			return new Credential(SharedJsonMapper.get().readTree(inputStream));
		}
	}

}
