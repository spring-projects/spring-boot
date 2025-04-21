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
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Supplier;

import org.springframework.boot.buildpack.platform.docker.configuration.DockerConfigurationMetadata.Auth;
import org.springframework.boot.buildpack.platform.docker.configuration.DockerConfigurationMetadata.DockerConfig;
import org.springframework.boot.buildpack.platform.docker.type.ImageReference;
import org.springframework.boot.buildpack.platform.system.Environment;
import org.springframework.util.StringUtils;
import org.springframework.util.function.SingletonSupplier;

/**
 * A default implementation of {@link DockerRegistryAuthentication} that provides
 * authentication using a Docker configuration file, leveraging both credential helpers
 * and static credentials.
 *
 * @author Dmytro Nosan
 */
class DefaultDockerRegistryAuthentication implements DockerRegistryAuthentication {

	private static final String DEFAULT_DOMAIN = "docker.io";

	private static final String INDEX_URL = "https://index.docker.io/v1/";

	private final Map<String, String> cache = new ConcurrentHashMap<>();

	private final Function<String, DockerCredentialHelper> dockerCredentialHelperFactory;

	private final Supplier<DockerConfig> dockerConfigSupplier;

	DefaultDockerRegistryAuthentication() {
		this(Environment.SYSTEM, DockerCredentialHelper::ofSuffix);
	}

	/**
	 * Creates a {@code DockerConfigFileDockerRegistryAuthentication} instance using the
	 * provided {@link Environment} and {@link DockerCredentialHelper} factory.
	 * @param environment the environment from which to retrieve environment variables
	 * @param dockerCredentialHelperFactory the factory to create a
	 * {@link DockerCredentialHelper} instance based on the provided credential helper
	 * name. The factory is invoked with the credential helper name. For example,
	 * {@code desktop}, {@code osxkeychain}, etc.
	 */
	DefaultDockerRegistryAuthentication(Environment environment,
			Function<String, DockerCredentialHelper> dockerCredentialHelperFactory) {
		this.dockerConfigSupplier = SingletonSupplier
			.of(() -> DockerConfigurationMetadata.from(environment).getConfiguration());
		this.dockerCredentialHelperFactory = dockerCredentialHelperFactory;
	}

	@Override
	public String getAuthHeader(ImageReference imageReference) {
		// TODO: Should the authentication header be cached? The Docker CLI does not cache
		// it, whereas testcontainers does. Is caching safe in this context?
		return this.cache.computeIfAbsent(getServerUrl(imageReference), (serverUrl) -> {
			DockerConfig dockerConfig = this.dockerConfigSupplier.get();
			return getAuthentication(dockerConfig, serverUrl).getAuthHeader(imageReference);
		});
	}

	private String getServerUrl(ImageReference imageReference) {
		String domain = imageReference.getDomain();
		return DEFAULT_DOMAIN.equals(domain) ? INDEX_URL : domain;
	}

	private DockerRegistryAuthentication getAuthentication(DockerConfig dockerConfig, String serverUrl) {
		RegistryAuth auth = getAuth(dockerConfig, serverUrl);
		Credentials credentials = getCredentials(dockerConfig, serverUrl);
		if (credentials != null && credentials.isIdentityToken()) {
			return new DockerRegistryTokenAuthentication(credentials.getSecret());
		}
		if (credentials != null) {
			return new DockerRegistryUserAuthentication(credentials.getUsername(), credentials.getSecret(),
					(credentials.getServerUrl() != null) ? credentials.getServerUrl() : serverUrl,
					(auth != null) ? auth.email() : null);
		}
		if (auth != null) {
			return new DockerRegistryUserAuthentication(auth.username(), auth.password(), auth.serverUrl(),
					auth.email());
		}
		return new DockerRegistryUserAuthentication("", "", "", "");
	}

	private Credentials getCredentials(DockerConfig dockerConfig, String serverUrl) {
		try {
			String helper = dockerConfig.getCredHelpers().getOrDefault(serverUrl, dockerConfig.getCredsStore());
			return StringUtils.hasText(helper) ? this.dockerCredentialHelperFactory.apply(helper).get(serverUrl) : null;
		}
		catch (IOException ex) {
			System.err.printf("Error retrieving credentials for '%s' due to: %s%n", serverUrl, ex.getMessage());
		}
		return null;
	}

	private RegistryAuth getAuth(DockerConfig dockerConfig, String serverUrl) {
		return dockerConfig.getAuths()
			.entrySet()
			.stream()
			.filter((entry) -> entry.getKey().equals(serverUrl) || entry.getKey().endsWith("://" + serverUrl))
			.map((entry) -> new RegistryAuth(entry.getKey(), entry.getValue()))
			.findFirst()
			.orElse(null);
	}

	private record RegistryAuth(String serverUrl, String username, String password, String email) {
		private RegistryAuth(String serverUrl, Auth auth) {
			this(serverUrl, auth.getUsername(), auth.getPassword(), auth.getEmail());
		}
	}

}
