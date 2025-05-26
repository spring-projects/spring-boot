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
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import com.fasterxml.jackson.core.type.TypeReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.springframework.boot.buildpack.platform.docker.type.ImageReference;
import org.springframework.boot.buildpack.platform.json.SharedObjectMapper;
import org.springframework.boot.testsupport.classpath.resources.ResourcesRoot;
import org.springframework.boot.testsupport.classpath.resources.WithResource;
import org.springframework.boot.testsupport.system.OutputCaptureExtension;
import org.springframework.core.io.ClassPathResource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;

/**
 * Tests for {@link DockerRegistryConfigAuthentication}.
 *
 * @author Dmytro Nosan
 * @author Phillip Webb
 */
@ExtendWith(OutputCaptureExtension.class)
class DockerRegistryConfigAuthenticationTests {

	private final Map<String, String> environment = new LinkedHashMap<>();

	private final Map<String, Exception> helperExceptions = new LinkedHashMap<>();

	private final Map<String, CredentialHelper> credentialHelpers = new HashMap<>();

	@BeforeEach
	void cleanup() {
		DockerRegistryConfigAuthentication.credentialFromHelperCache.clear();
	}

	@WithResource(name = "config.json", content = """
			{
			  "auths": {
			    "https://index.docker.io/v1/": {
			      "auth": "dXNlcm5hbWU6cGFzc3dvcmQ=",
			      "email": "test@example.com"
			    }
			  }
			}
			""")
	@Test
	void getAuthHeaderWhenAuthForDockerDomain(@ResourcesRoot Path directory) throws Exception {
		this.environment.put("DOCKER_CONFIG", directory.toString());
		ImageReference imageReference = ImageReference.of("docker.io/ubuntu:latest");
		String authHeader = getAuthHeader(imageReference);
		assertThat(decode(authHeader)).hasSize(4)
			.containsEntry("serveraddress", "https://index.docker.io/v1/")
			.containsEntry("username", "username")
			.containsEntry("password", "password")
			.containsEntry("email", "test@example.com");
	}

	@WithResource(name = "config.json", content = """
			{
			  "auths": {
			    "https://index.docker.io/v1/": {
			      "auth": "dXNlcm5hbWU6cGFzc3dvcmQ=",
			      "email": "test@example.com"
			    }
			  }
			}
			""")
	@Test
	void getAuthHeaderWhenAuthForLegacyDockerDomain(@ResourcesRoot Path directory) throws Exception {
		this.environment.put("DOCKER_CONFIG", directory.toString());
		ImageReference imageReference = ImageReference.of("index.docker.io/ubuntu:latest");
		String authHeader = getAuthHeader(imageReference);
		assertThat(decode(authHeader)).hasSize(4)
			.containsEntry("serveraddress", "https://index.docker.io/v1/")
			.containsEntry("username", "username")
			.containsEntry("password", "password")
			.containsEntry("email", "test@example.com");
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
	void getAuthHeaderWhenAuthForCustomDomain(@ResourcesRoot Path directory) throws Exception {
		this.environment.put("DOCKER_CONFIG", directory.toString());
		ImageReference imageReference = ImageReference.of("my-registry.example.com/ubuntu:latest");
		String authHeader = getAuthHeader(imageReference);
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
	void getAuthHeaderWhenAuthForCustomDomainWithLegacyFormat(@ResourcesRoot Path directory) throws Exception {
		this.environment.put("DOCKER_CONFIG", directory.toString());
		ImageReference imageReference = ImageReference.of("my-registry.example.com/ubuntu:latest");
		String authHeader = getAuthHeader(imageReference);
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
	void getAuthHeaderWhenEmptyConfigDirectoryReturnsFallback(@ResourcesRoot Path directory) throws Exception {
		this.environment.put("DOCKER_CONFIG", directory.toString());
		ImageReference imageReference = ImageReference.of("docker.io/ubuntu:latest");
		String authHeader = getAuthHeader(imageReference, DockerRegistryAuthentication.EMPTY_USER);
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
	void getAuthHeaderWhenUsingHelperFromCredsStore(@ResourcesRoot Path directory) throws Exception {
		this.environment.put("DOCKER_CONFIG", directory.toString());
		ImageReference imageReference = ImageReference.of("docker.io/ubuntu:latest");
		mockHelper("desktop", "https://index.docker.io/v1/", "credentials.json");
		String authHeader = getAuthHeader(imageReference);
		assertThat(decode(authHeader)).hasSize(1).containsEntry("identitytoken", "secret");
	}

	@WithResource(name = "config.json", content = """
			{
			  "auths": {
			    "gcr.io": {
			      "email": "test@example.com"
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
	void getAuthHeaderWhenUsingHelperFromCredsStoreAndUseEmailFromAuth(@ResourcesRoot Path directory) throws Exception {
		this.environment.put("DOCKER_CONFIG", directory.toString());
		ImageReference imageReference = ImageReference.of("gcr.io/ubuntu:latest");
		mockHelper("gcr", "gcr.io", "credentials.json");
		String authHeader = getAuthHeader(imageReference);
		assertThat(decode(authHeader)).hasSize(4)
			.containsEntry("serveraddress", "https://my-gcr.io")
			.containsEntry("username", "username")
			.containsEntry("password", "secret")
			.containsEntry("email", "test@example.com");
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
	void getAuthHeaderWhenUsingHelperFromCredHelpersUsesProvidedServerUrl(@ResourcesRoot Path directory)
			throws Exception {
		this.environment.put("DOCKER_CONFIG", directory.toString());
		ImageReference imageReference = ImageReference.of("gcr.io/ubuntu:latest");
		mockHelper("gcr", "gcr.io", "credentials.json");
		String authHeader = getAuthHeader(imageReference);
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
			      "email": "test@example.com"
			    }
			  },
			  "credsStore": "desktop",
			  "credHelpers": {
			    "gcr.io": "gcr"
			  }
			}
			""")
	@Test
	void getAuthHeaderWhenUsingHelperThatFailsLogsErrorAndReturnsFromAuths(@ResourcesRoot Path directory)
			throws Exception {
		this.environment.put("DOCKER_CONFIG", directory.toString());
		ImageReference imageReference = ImageReference.of("gcr.io/ubuntu:latest");
		CredentialHelper helper = mockHelper("gcr");
		given(helper.get("gcr.io")).willThrow(new IOException("Failed to obtain credentials for registry"));
		String authHeader = getAuthHeader(imageReference);
		assertThat(decode(authHeader)).hasSize(4)
			.containsEntry("serveraddress", "gcr.io")
			.containsEntry("username", "username")
			.containsEntry("password", "password")
			.containsEntry("email", "test@example.com");
		assertThat(this.helperExceptions).hasSize(1);
		assertThat(this.helperExceptions.keySet().iterator().next())
			.contains("Error retrieving credentials for 'gcr.io' due to: Failed to obtain credentials for registry");
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
	void getAuthHeaderWhenUsingHelperThatFailsAndNoAuthLogsErrorAndReturnsFallback(@ResourcesRoot Path directory)
			throws Exception {
		this.environment.put("DOCKER_CONFIG", directory.toString());
		ImageReference imageReference = ImageReference.of("gcr.io/ubuntu:latest");
		CredentialHelper helper = mockHelper("gcr");
		given(helper.get("gcr.io")).willThrow(new IOException("Failed to obtain credentials for registry"));
		String authHeader = getAuthHeader(imageReference, DockerRegistryAuthentication.EMPTY_USER);
		assertThat(this.helperExceptions).hasSize(1);
		assertThat(this.helperExceptions.keySet().iterator().next())
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
	@Test
	void getAuthHeaderWhenEmptyCredHelperReturnsFallbackAndDoesNotUseCredStore(@ResourcesRoot Path directory)
			throws Exception {
		this.environment.put("DOCKER_CONFIG", directory.toString());
		ImageReference imageReference = ImageReference.of("gcr.io/ubuntu:latest");
		CredentialHelper desktopHelper = mockHelper("desktop");
		String authHeader = getAuthHeader(imageReference, DockerRegistryAuthentication.EMPTY_USER);
		// The Docker CLI appears to prioritize the credential helper over the
		// credential store, even when the helper is empty.
		assertThat(decode(authHeader)).hasSize(4)
			.containsEntry("serveraddress", "")
			.containsEntry("username", "")
			.containsEntry("password", "")
			.containsEntry("email", "");
		then(desktopHelper).should(never()).get(any(String.class));
	}

	@WithResource(name = "config.json", content = """
			{
			  "credsStore": "desktop"
			}
			""")
	@Test
	void getAuthHeaderReturnsFallbackWhenImageReferenceNull(@ResourcesRoot Path directory) throws Exception {
		this.environment.put("DOCKER_CONFIG", directory.toString());
		CredentialHelper desktopHelper = mockHelper("desktop");
		String authHeader = getAuthHeader(null, DockerRegistryAuthentication.EMPTY_USER);
		assertThat(decode(authHeader)).hasSize(4)
			.containsEntry("serveraddress", "")
			.containsEntry("username", "")
			.containsEntry("password", "")
			.containsEntry("email", "");
		then(desktopHelper).should(never()).get(any(String.class));
	}

	@WithResource(name = "config.json", content = """
			{
			  "auths": {
			    "https://my-registry.example.com": {
			      "email": "test@example.com"
			    }
			  },
			  "credsStore": "desktop"
			}
			""")
	@WithResource(name = "credentials.json", content = """
			{
			  "Username": "username",
			  "Secret": "secret"
			}
			""")
	@Test
	void getAuthHeaderWhenUsingHelperFromCredHelpersUsesImageReferenceServerUrlAsFallback(@ResourcesRoot Path directory)
			throws Exception {
		this.environment.put("DOCKER_CONFIG", directory.toString());
		mockHelper("desktop", "my-registry.example.com", "credentials.json");
		ImageReference imageReference = ImageReference.of("my-registry.example.com/ubuntu:latest");
		String authHeader = getAuthHeader(imageReference);
		assertThat(decode(authHeader)).hasSize(4)
			.containsEntry("serveraddress", "my-registry.example.com")
			.containsEntry("username", "username")
			.containsEntry("password", "secret")
			.containsEntry("email", "test@example.com");
	}

	private String getAuthHeader(ImageReference imageReference) {
		return getAuthHeader(imageReference, null);
	}

	private String getAuthHeader(ImageReference imageReference, DockerRegistryAuthentication fallback) {
		DockerRegistryConfigAuthentication authentication = getAuthentication(fallback);
		return authentication.getAuthHeader(imageReference);
	}

	private DockerRegistryConfigAuthentication getAuthentication(DockerRegistryAuthentication fallback) {
		return new DockerRegistryConfigAuthentication(fallback, this.helperExceptions::put, this.environment::get,
				this.credentialHelpers::get);
	}

	private void mockHelper(String name, String serverUrl, String credentialsResourceName) throws Exception {
		CredentialHelper helper = mockHelper(name);
		given(helper.get(serverUrl)).willReturn(getCredentials(credentialsResourceName));
	}

	private CredentialHelper mockHelper(String name) {
		CredentialHelper helper = mock(CredentialHelper.class);
		this.credentialHelpers.put(name, helper);
		return helper;
	}

	private Credential getCredentials(String resourceName) throws Exception {
		try (InputStream inputStream = new ClassPathResource(resourceName).getInputStream()) {
			return new Credential(SharedObjectMapper.get().readTree(inputStream));
		}
	}

	private Map<String, String> decode(String authHeader) throws Exception {
		assertThat(authHeader).isNotNull();
		return SharedObjectMapper.get().readValue(Base64.getDecoder().decode(authHeader), new TypeReference<>() {
		});
	}

}
