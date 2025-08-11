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

package org.springframework.boot.maven;

import org.apache.maven.plugin.logging.Log;
import org.jspecify.annotations.Nullable;

import org.springframework.boot.buildpack.platform.build.BuilderDockerConfiguration;
import org.springframework.boot.buildpack.platform.docker.configuration.DockerRegistryAuthentication;
import org.springframework.util.Assert;

/**
 * Docker configuration options.
 *
 * @author Wei Jiang
 * @author Scott Frederick
 * @since 2.4.0
 */
public class Docker {

	private @Nullable String host;

	private @Nullable String context;

	private boolean tlsVerify;

	private @Nullable String certPath;

	private boolean bindHostToBuilder;

	private @Nullable DockerRegistry builderRegistry;

	private @Nullable DockerRegistry publishRegistry;

	/**
	 * The host address of the Docker daemon.
	 * @return the Docker host
	 */
	public @Nullable String getHost() {
		return this.host;
	}

	void setHost(@Nullable String host) {
		this.host = host;
	}

	/**
	 * The Docker context to use to retrieve host configuration.
	 * @return the Docker context
	 */
	public @Nullable String getContext() {
		return this.context;
	}

	public void setContext(@Nullable String context) {
		this.context = context;
	}

	/**
	 * Whether the Docker daemon requires TLS communication.
	 * @return {@code true} to enable TLS
	 */
	public boolean isTlsVerify() {
		return this.tlsVerify;
	}

	void setTlsVerify(boolean tlsVerify) {
		this.tlsVerify = tlsVerify;
	}

	/**
	 * The path to TLS certificate and key files required for TLS communication with the
	 * Docker daemon.
	 * @return the TLS certificate path
	 */
	public @Nullable String getCertPath() {
		return this.certPath;
	}

	void setCertPath(@Nullable String certPath) {
		this.certPath = certPath;
	}

	/**
	 * Whether to use the configured Docker host in the builder container.
	 * @return {@code true} to use the configured Docker host in the builder container
	 */
	public boolean isBindHostToBuilder() {
		return this.bindHostToBuilder;
	}

	void setBindHostToBuilder(boolean bindHostToBuilder) {
		this.bindHostToBuilder = bindHostToBuilder;
	}

	/**
	 * Configuration of the Docker registry where builder and run images are stored.
	 * @return the registry configuration
	 */
	@Nullable DockerRegistry getBuilderRegistry() {
		return this.builderRegistry;
	}

	/**
	 * Sets the {@link DockerRegistry} that configures authentication to the builder
	 * registry.
	 * @param builderRegistry the registry configuration
	 */
	void setBuilderRegistry(@Nullable DockerRegistry builderRegistry) {
		this.builderRegistry = builderRegistry;
	}

	/**
	 * Configuration of the Docker registry where the generated image will be published.
	 * @return the registry configuration
	 */
	@Nullable DockerRegistry getPublishRegistry() {
		return this.publishRegistry;
	}

	/**
	 * Sets the {@link DockerRegistry} that configures authentication to the publishing
	 * registry.
	 * @param builderRegistry the registry configuration
	 */
	void setPublishRegistry(@Nullable DockerRegistry builderRegistry) {
		this.publishRegistry = builderRegistry;
	}

	/**
	 * Returns this configuration as a {@link BuilderDockerConfiguration} instance. This
	 * method should only be called when the configuration is complete and will no longer
	 * be changed.
	 * @param log the output log
	 * @param publish whether the image should be published
	 * @return the Docker configuration
	 */
	BuilderDockerConfiguration asDockerConfiguration(Log log, boolean publish) {
		BuilderDockerConfiguration dockerConfiguration = new BuilderDockerConfiguration();
		dockerConfiguration = customizeHost(dockerConfiguration);
		dockerConfiguration = dockerConfiguration.withBindHostToBuilder(this.bindHostToBuilder);
		dockerConfiguration = customizeBuilderAuthentication(log, dockerConfiguration);
		dockerConfiguration = customizePublishAuthentication(log, dockerConfiguration, publish);
		return dockerConfiguration;
	}

	private BuilderDockerConfiguration customizeHost(BuilderDockerConfiguration dockerConfiguration) {
		if (this.context != null && this.host != null) {
			throw new IllegalArgumentException(
					"Invalid Docker configuration, either context or host can be provided but not both");
		}
		if (this.context != null) {
			return dockerConfiguration.withContext(this.context);
		}
		if (this.host != null) {
			return dockerConfiguration.withHost(this.host, this.tlsVerify, this.certPath);
		}
		return dockerConfiguration;
	}

	private BuilderDockerConfiguration customizeBuilderAuthentication(Log log,
			BuilderDockerConfiguration dockerConfiguration) {
		DockerRegistryAuthentication authentication = DockerRegistryAuthentication.configuration(null,
				(message, ex) -> log.warn(message));
		return dockerConfiguration.withBuilderRegistryAuthentication(
				getRegistryAuthentication("builder", this.builderRegistry, authentication));
	}

	private BuilderDockerConfiguration customizePublishAuthentication(Log log,
			BuilderDockerConfiguration dockerConfiguration, boolean publish) {
		if (!publish) {
			return dockerConfiguration;
		}
		DockerRegistryAuthentication authentication = DockerRegistryAuthentication
			.configuration(DockerRegistryAuthentication.EMPTY_USER, (message, ex) -> log.warn(message));
		return dockerConfiguration.withPublishRegistryAuthentication(
				getRegistryAuthentication("publish", this.publishRegistry, authentication));
	}

	private DockerRegistryAuthentication getRegistryAuthentication(String type, @Nullable DockerRegistry registry,
			DockerRegistryAuthentication fallback) {
		if (registry == null || registry.isEmpty()) {
			return fallback;
		}
		if (registry.hasTokenAuth() && !registry.hasUserAuth()) {
			String token = registry.getToken();
			Assert.state(token != null, "'token' must not be null");
			return DockerRegistryAuthentication.token(token);
		}
		if (registry.hasUserAuth() && !registry.hasTokenAuth()) {
			String username = registry.getUsername();
			String password = registry.getPassword();
			Assert.state(username != null, "'username' must not be null");
			Assert.state(password != null, "'password' must not be null");
			return DockerRegistryAuthentication.user(username, password, registry.getUrl(), registry.getEmail());
		}
		throw new IllegalArgumentException("Invalid Docker " + type
				+ " registry configuration, either token or username/password must be provided");
	}

	/**
	 * Encapsulates Docker registry authentication configuration options.
	 */
	public static class DockerRegistry {

		private @Nullable String username;

		private @Nullable String password;

		private @Nullable String url;

		private @Nullable String email;

		private @Nullable String token;

		public DockerRegistry() {
		}

		public DockerRegistry(String username, String password, String url, String email) {
			this.username = username;
			this.password = password;
			this.url = url;
			this.email = email;
		}

		public DockerRegistry(String token) {
			this.token = token;
		}

		/**
		 * The username that will be used for user authentication to the registry.
		 * @return the username
		 */
		public @Nullable String getUsername() {
			return this.username;
		}

		void setUsername(@Nullable String username) {
			this.username = username;
		}

		/**
		 * The password that will be used for user authentication to the registry.
		 * @return the password
		 */
		public @Nullable String getPassword() {
			return this.password;
		}

		void setPassword(@Nullable String password) {
			this.password = password;
		}

		/**
		 * The email address that will be used for user authentication to the registry.
		 * @return the email address
		 */
		public @Nullable String getEmail() {
			return this.email;
		}

		void setEmail(@Nullable String email) {
			this.email = email;
		}

		/**
		 * The URL of the registry.
		 * @return the registry URL
		 */
		@Nullable String getUrl() {
			return this.url;
		}

		void setUrl(@Nullable String url) {
			this.url = url;
		}

		/**
		 * The token that will be used for token authentication to the registry.
		 * @return the authentication token
		 */
		public @Nullable String getToken() {
			return this.token;
		}

		void setToken(@Nullable String token) {
			this.token = token;
		}

		boolean isEmpty() {
			return this.username == null && this.password == null && this.url == null && this.email == null
					&& this.token == null;
		}

		boolean hasTokenAuth() {
			return this.token != null;
		}

		boolean hasUserAuth() {
			return this.username != null && this.password != null;
		}

	}

}
