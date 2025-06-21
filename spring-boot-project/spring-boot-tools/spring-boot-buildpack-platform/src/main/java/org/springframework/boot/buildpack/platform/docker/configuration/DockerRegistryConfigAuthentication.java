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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.Function;

import org.springframework.boot.buildpack.platform.docker.configuration.DockerConfigurationMetadata.Auth;
import org.springframework.boot.buildpack.platform.docker.configuration.DockerConfigurationMetadata.DockerConfig;
import org.springframework.boot.buildpack.platform.docker.type.ImageReference;
import org.springframework.boot.buildpack.platform.system.Environment;
import org.springframework.util.StringUtils;

/**
 * {@link DockerRegistryAuthentication} for
 * {@link DockerRegistryAuthentication#configuration(DockerRegistryAuthentication, BiConsumer)}.
 *
 * @author Dmytro Nosan
 * @author Phillip Webb
 */
class DockerRegistryConfigAuthentication implements DockerRegistryAuthentication {

	private static final String DEFAULT_DOMAIN = "docker.io";

	private static final String INDEX_URL = "https://index.docker.io/v1/";

	static Map<String, Credential> credentialFromHelperCache = new ConcurrentHashMap<>();

	private final DockerRegistryAuthentication fallback;

	private final BiConsumer<String, Exception> credentialHelperExceptionHandler;

	private final Function<String, CredentialHelper> credentialHelperFactory;

	private final DockerConfig dockerConfig;

	DockerRegistryConfigAuthentication(DockerRegistryAuthentication fallback,
			BiConsumer<String, Exception> credentialHelperExceptionHandler) {
		this(fallback, credentialHelperExceptionHandler, Environment.SYSTEM,
				(helper) -> new CredentialHelper("docker-credential-" + helper));
	}

	DockerRegistryConfigAuthentication(DockerRegistryAuthentication fallback,
			BiConsumer<String, Exception> credentialHelperExceptionHandler, Environment environment,
			Function<String, CredentialHelper> credentialHelperFactory) {
		this.fallback = fallback;
		this.credentialHelperExceptionHandler = credentialHelperExceptionHandler;
		this.dockerConfig = DockerConfigurationMetadata.from(environment).getConfiguration();
		this.credentialHelperFactory = credentialHelperFactory;
	}

	@Override
	public String getAuthHeader() {
		return getAuthHeader(null);
	}

	@Override
	public String getAuthHeader(ImageReference imageReference) {
		String serverUrl = getServerUrl(imageReference);
		DockerRegistryAuthentication authentication = getAuthentication(serverUrl);
		return (authentication != null) ? authentication.getAuthHeader(imageReference) : null;
	}

	private String getServerUrl(ImageReference imageReference) {
		String domain = (imageReference != null) ? imageReference.getDomain() : null;
		return (!DEFAULT_DOMAIN.equals(domain)) ? domain : INDEX_URL;
	}

	private DockerRegistryAuthentication getAuthentication(String serverUrl) {
		Credential credentialsFromHelper = getCredentialsFromHelper(serverUrl);
		Map.Entry<String, Auth> authConfigEntry = getAuthConfigEntry(serverUrl);
		Auth authConfig = (authConfigEntry != null) ? authConfigEntry.getValue() : null;
		if (credentialsFromHelper != null) {
			return getAuthentication(credentialsFromHelper, authConfig, serverUrl);
		}
		if (authConfig != null) {
			return DockerRegistryAuthentication.user(authConfig.getUsername(), authConfig.getPassword(),
					authConfigEntry.getKey(), authConfig.getEmail());
		}
		return this.fallback;
	}

	private DockerRegistryAuthentication getAuthentication(Credential credentialsFromHelper, Auth authConfig,
			String serverUrl) {
		if (credentialsFromHelper.isIdentityToken()) {
			return DockerRegistryAuthentication.token(credentialsFromHelper.getSecret());
		}
		String username = credentialsFromHelper.getUsername();
		String password = credentialsFromHelper.getSecret();
		String serverAddress = (StringUtils.hasLength(credentialsFromHelper.getServerUrl()))
				? credentialsFromHelper.getServerUrl() : serverUrl;
		String email = (authConfig != null) ? authConfig.getEmail() : null;
		return DockerRegistryAuthentication.user(username, password, serverAddress, email);
	}

	private Credential getCredentialsFromHelper(String serverUrl) {
		return StringUtils.hasLength(serverUrl)
				? credentialFromHelperCache.computeIfAbsent(serverUrl, this::computeCredentialsFromHelper) : null;
	}

	private Credential computeCredentialsFromHelper(String serverUrl) {
		CredentialHelper credentialHelper = getCredentialHelper(serverUrl);
		if (credentialHelper != null) {
			try {
				return credentialHelper.get(serverUrl);
			}
			catch (Exception ex) {
				String message = "Error retrieving credentials for '%s' due to: %s".formatted(serverUrl,
						ex.getMessage());
				this.credentialHelperExceptionHandler.accept(message, ex);
			}
		}
		return null;
	}

	private CredentialHelper getCredentialHelper(String serverUrl) {
		String name = this.dockerConfig.getCredHelpers().getOrDefault(serverUrl, this.dockerConfig.getCredsStore());
		return (StringUtils.hasLength(name)) ? this.credentialHelperFactory.apply(name) : null;
	}

	private Map.Entry<String, Auth> getAuthConfigEntry(String serverUrl) {
		for (Map.Entry<String, Auth> candidate : this.dockerConfig.getAuths().entrySet()) {
			if (candidate.getKey().equals(serverUrl) || candidate.getKey().endsWith("://" + serverUrl)) {
				return candidate;
			}
		}
		return null;
	}

}
