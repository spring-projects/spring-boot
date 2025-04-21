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
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

import com.fasterxml.jackson.core.type.TypeReference;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.springframework.boot.buildpack.platform.docker.type.ImageReference;
import org.springframework.boot.buildpack.platform.json.SharedObjectMapper;
import org.springframework.boot.testsupport.classpath.resources.ResourcesRoot;
import org.springframework.boot.testsupport.classpath.resources.WithResource;
import org.springframework.boot.testsupport.system.CapturedOutput;
import org.springframework.boot.testsupport.system.OutputCaptureExtension;
import org.springframework.core.io.ClassPathResource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link DefaultDockerRegistryAuthentication}.
 *
 * @author Dmytro Nosan
 */
@ExtendWith(OutputCaptureExtension.class)
class DefaultDockerRegistryAuthenticationTests {

	private final Map<String, String> environment = new LinkedHashMap<>();

	private final Map<String, DockerCredentialHelper> dockerCredentialHelpers = new LinkedHashMap<>();

	private final DefaultDockerRegistryAuthentication authentication = new DefaultDockerRegistryAuthentication(
			this.environment::get, this.dockerCredentialHelpers::get);

	@WithResource(name = "config.json", content = """
			{
			  "auths": {
			    "https://index.docker.io/v1/": {
			      "auth": "dXNlcm5hbWU6cGFzc3dvcmQ=",
			      "email": "test@gmail.com"
			    }
			  }
			}
			""")
	@Test
	void shouldCreateAuthHeaderFromAuthForDockerDomain(@ResourcesRoot Path directory) throws IOException {
		this.environment.put("DOCKER_CONFIG", directory.toString());
		ImageReference imageReference = ImageReference.of("docker.io/ubuntu:latest");
		String authHeader = this.authentication.getAuthHeader(imageReference);
		assertThat(decode(authHeader)).hasSize(4)
			.containsEntry("serveraddress", "https://index.docker.io/v1/")
			.containsEntry("username", "username")
			.containsEntry("password", "password")
			.containsEntry("email", "test@gmail.com");
	}

	@WithResource(name = "config.json", content = """
			{
			  "auths": {
			    "https://index.docker.io/v1/": {
			      "auth": "dXNlcm5hbWU6cGFzc3dvcmQ=",
			      "email": "test@gmail.com"
			    }
			  }
			}
			""")
	@Test
	void shouldCreateAuthHeaderFromAuthForLegacyDockerDomain(@ResourcesRoot Path directory) throws IOException {
		this.environment.put("DOCKER_CONFIG", directory.toString());
		ImageReference imageReference = ImageReference.of("index.docker.io/ubuntu:latest");
		String authHeader = this.authentication.getAuthHeader(imageReference);
		assertThat(decode(authHeader)).hasSize(4)
			.containsEntry("serveraddress", "https://index.docker.io/v1/")
			.containsEntry("username", "username")
			.containsEntry("password", "password")
			.containsEntry("email", "test@gmail.com");
	}

	@WithResource(name = "config.json", content = """
			{
			  "auths": {
				"my-registry.example.com": {
				  "auth": "Y3VzdG9tVXNlcjpjdXN0b21QYXNz"
				}
			  }
			}
			""")
	@Test
	void shouldCreateAuthHeaderFromAuthForCustomDomain(@ResourcesRoot Path directory) throws IOException {
		this.environment.put("DOCKER_CONFIG", directory.toString());
		ImageReference imageReference = ImageReference.of("my-registry.example.com/ubuntu:latest");
		String authHeader = this.authentication.getAuthHeader(imageReference);
		assertThat(decode(authHeader)).hasSize(4)
			.containsEntry("serveraddress", "my-registry.example.com")
			.containsEntry("username", "customUser")
			.containsEntry("password", "customPass")
			.containsEntry("email", null);
	}

	@WithResource(name = "config.json", content = """
			{
			  "auths": {
				"https://my-registry.example.com": {
				  "auth": "Y3VzdG9tVXNlcjpjdXN0b21QYXNz"
				}
			  }
			}
			""")
	@Test
	void shouldCreateAuthHeaderFromAuthForCustomDomainWithLegacyFormat(@ResourcesRoot Path directory)
			throws IOException {
		this.environment.put("DOCKER_CONFIG", directory.toString());
		ImageReference imageReference = ImageReference.of("my-registry.example.com/ubuntu:latest");
		String authHeader = this.authentication.getAuthHeader(imageReference);
		assertThat(decode(authHeader)).hasSize(4)
			.containsEntry("serveraddress", "https://my-registry.example.com")
			.containsEntry("username", "customUser")
			.containsEntry("password", "customPass")
			.containsEntry("email", null);
	}

	@WithResource(name = "config.json", content = """
			{
			}
			""")
	@Test
	void shouldCreateAuthHeaderFromEmptyCredentialsWhenEmptyConfig(@ResourcesRoot Path directory) throws IOException {
		this.environment.put("DOCKER_CONFIG", directory.toString());
		ImageReference imageReference = ImageReference.of("docker.io/ubuntu:latest");
		String authHeader = this.authentication.getAuthHeader(imageReference);
		assertThat(decode(authHeader)).hasSize(4)
			.containsEntry("serveraddress", "")
			.containsEntry("username", "")
			.containsEntry("password", "")
			.containsEntry("email", "");
	}

	@WithResource(name = "config.json", content = """
				{
				  "credsStore": "desktop"
				}
			""")
	@WithResource(name = "credentials.json", content = """
			{
			  "ServerURL": "https://index.docker.io/v1/",
			  "Username": "<token>",
			  "Secret": "secret"
			}
			""")
	@Test
	void shouldCreateAuthHeaderFromCredsStore(@ResourcesRoot Path directory) throws IOException {
		this.environment.put("DOCKER_CONFIG", directory.toString());
		ImageReference imageReference = ImageReference.of("docker.io/ubuntu:latest");
		DockerCredentialHelper helper = mock(DockerCredentialHelper.class);
		this.dockerCredentialHelpers.put("desktop", helper);
		given(helper.get("https://index.docker.io/v1/")).willReturn(getCredentials("credentials.json"));
		String authHeader = this.authentication.getAuthHeader(imageReference);
		assertThat(decode(authHeader)).hasSize(1).containsEntry("identitytoken", "secret");
	}

	@WithResource(name = "config.json", content = """
			{
			  "auths": {
			    "gcr.io": {
			      "email": "test@gmail.com"
			    }
			  },
			  "credsStore": "desktop",
			  "credHelpers": {
			    "gcr.io": "gcr"
			  }
			}
			""")
	@WithResource(name = "credentials.json", content = """
			{
			  "ServerURL": "https://my-gcr.io",
			  "Username": "username",
			  "Secret": "secret"
			}
			""")
	@Test
	void shouldCreateAuthHeaderFromCredHelperAndUseEmailFromAuth(@ResourcesRoot Path directory) throws IOException {
		this.environment.put("DOCKER_CONFIG", directory.toString());
		ImageReference imageReference = ImageReference.of("gcr.io/ubuntu:latest");
		DockerCredentialHelper helper = mock(DockerCredentialHelper.class);
		this.dockerCredentialHelpers.put("gcr", helper);
		given(helper.get("gcr.io")).willReturn(getCredentials("credentials.json"));
		String authHeader = this.authentication.getAuthHeader(imageReference);
		assertThat(decode(authHeader)).hasSize(4)
			.containsEntry("serveraddress", "https://my-gcr.io")
			.containsEntry("username", "username")
			.containsEntry("password", "secret")
			.containsEntry("email", "test@gmail.com");
	}

	@WithResource(name = "config.json", content = """
			{
			  "credsStore": "desktop",
			  "credHelpers": {
			    "gcr.io": "gcr"
			  }
			}
			""")
	@WithResource(name = "credentials.json", content = """
			{
			  "Username": "username",
			  "Secret": "secret"
			}
			""")
	@Test
	void shouldCreateAuthHeaderFromCredHelperAndUseProvidedServerUrl(@ResourcesRoot Path directory) throws IOException {
		this.environment.put("DOCKER_CONFIG", directory.toString());
		ImageReference imageReference = ImageReference.of("gcr.io/ubuntu:latest");
		DockerCredentialHelper helper = mock(DockerCredentialHelper.class);
		this.dockerCredentialHelpers.put("gcr", helper);
		given(helper.get("gcr.io")).willReturn(getCredentials("credentials.json"));
		String authHeader = this.authentication.getAuthHeader(imageReference);
		assertThat(decode(authHeader)).hasSize(4)
			.containsEntry("serveraddress", "gcr.io")
			.containsEntry("username", "username")
			.containsEntry("password", "secret")
			.containsEntry("email", null);
	}

	@WithResource(name = "config.json", content = """
			{
			"auths": {
			    "gcr.io": {
			      "auth": "dXNlcm5hbWU6cGFzc3dvcmQ=",
			      "email": "test@gmail.com"
			    }
			  },
			  "credsStore": "desktop",
			  "credHelpers": {
			    "gcr.io": "gcr"
			  }
			}
			""")
	@Test
	void shouldCreateHeaderFromAuthWhenFailedToGetCredentials(@ResourcesRoot Path directory, CapturedOutput output)
			throws IOException {
		this.environment.put("DOCKER_CONFIG", directory.toString());
		ImageReference imageReference = ImageReference.of("gcr.io/ubuntu:latest");
		DockerCredentialHelper helper = mock(DockerCredentialHelper.class);
		this.dockerCredentialHelpers.put("gcr", helper);
		given(helper.get("gcr.io")).willThrow(new IOException("Failed to obtain credentials for registry"));
		String authHeader = this.authentication.getAuthHeader(imageReference);
		assertThat(output.getErr())
			.contains("Error retrieving credentials for 'gcr.io' due to: Failed to obtain credentials for registry");
		assertThat(decode(authHeader)).hasSize(4)
			.containsEntry("serveraddress", "gcr.io")
			.containsEntry("username", "username")
			.containsEntry("password", "password")
			.containsEntry("email", "test@gmail.com");
	}

	@WithResource(name = "config.json", content = """
			{
			  "credsStore": "desktop",
			  "credHelpers": {
			    "gcr.io": "gcr"
			  }
			}
			""")
	@Test
	void shouldCreateAuthHeaderFromEmptyCredentialsWhenFailedToGetCredentials(@ResourcesRoot Path directory,
			CapturedOutput output) throws IOException {
		this.environment.put("DOCKER_CONFIG", directory.toString());
		ImageReference imageReference = ImageReference.of("gcr.io/ubuntu:latest");
		DockerCredentialHelper helper = mock(DockerCredentialHelper.class);
		this.dockerCredentialHelpers.put("gcr", helper);
		given(helper.get("gcr.io")).willThrow(new IOException("Failed to obtain credentials for registry"));
		String authHeader = this.authentication.getAuthHeader(imageReference);
		assertThat(output.getErr())
			.contains("Error retrieving credentials for 'gcr.io' due to: Failed to obtain credentials for registry");
		assertThat(decode(authHeader)).hasSize(4)
			.containsEntry("serveraddress", "")
			.containsEntry("username", "")
			.containsEntry("password", "")
			.containsEntry("email", "");
	}

	@WithResource(name = "config.json", content = """
			{
			  "credsStore": "desktop",
			  "credHelpers": {
			    "gcr.io": ""
			  }
			}
			""")
	// The Docker CLI appears to prioritize the credential helper over the
	// credential store, even when the helper is empty.
	@Test
	void shouldCreateAuthHeaderFromEmptyCredentialsWhenCredsHelperTakesPrecedenceOverCredsStoreAndNoAuth(
			@ResourcesRoot Path directory) throws IOException {
		this.environment.put("DOCKER_CONFIG", directory.toString());
		ImageReference imageReference = ImageReference.of("gcr.io/ubuntu:latest");
		String authHeader = this.authentication.getAuthHeader(imageReference);
		assertThat(decode(authHeader)).hasSize(4)
			.containsEntry("serveraddress", "")
			.containsEntry("username", "")
			.containsEntry("password", "")
			.containsEntry("email", "");
	}

	private Credentials getCredentials(String name) throws IOException {
		try (InputStream inputStream = new ClassPathResource(name).getInputStream()) {
			return new Credentials(SharedObjectMapper.get().readTree(inputStream));
		}
	}

	private Map<String, String> decode(String authHeader) throws IOException {
		assertThat(authHeader).isNotNull();
		return SharedObjectMapper.get().readValue(Base64.getDecoder().decode(authHeader), new TypeReference<>() {
		});
	}

}
