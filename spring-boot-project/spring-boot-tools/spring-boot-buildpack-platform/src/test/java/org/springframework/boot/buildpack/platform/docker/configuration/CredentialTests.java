/*
 * Copyright 2012-present the original author or authors.
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
import java.io.InputStream;

import org.junit.jupiter.api.Test;

import org.springframework.boot.buildpack.platform.json.SharedObjectMapper;
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
			return new Credential(SharedObjectMapper.get().readTree(inputStream));
		}
	}

}
