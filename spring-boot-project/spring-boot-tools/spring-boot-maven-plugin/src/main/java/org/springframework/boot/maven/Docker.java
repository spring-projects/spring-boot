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

package org.springframework.boot.maven;

import org.apache.maven.plugin.logging.Log;

import org.springframework.boot.buildpack.platform.build.BuilderDockerConfiguration;
import org.springframework.boot.buildpack.platform.docker.configuration.DockerRegistryAuthentication;

/**
 * Docker configuration options.
 *
 * @author Wei Jiang
 * @author Scott Frederick
 * @since 2.4.0
 */
public class Docker {

	private String host;

	private String context;

	private boolean tlsVerify;

	private String certPath;

	private boolean bindHostToBuilder;

	private DockerRegistry builderRegistry;

	private DockerRegistry publishRegistry;

	/**
	 * The host address of the Docker daemon.
	 * @return the Docker host
	 */
	public String getHost() {
		return this.host;
	}

	void setHost(String host) {
		this.host = host;
	}

	/**
	 * The Docker context to use to retrieve host configuration.
	 * @return the Docker context
	 */
	public String getContext() {
		return this.context;
	}

	public void setContext(String context) {
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
	public String getCertPath() {
		return this.certPath;
	}

	void setCertPath(String certPath) {
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
	DockerRegistry getBuilderRegistry() {
		return this.builderRegistry;
	}

	/**
	 * Sets the {@link DockerRegistry} that configures authentication to the builder
	 * registry.
	 * @param builderRegistry the registry configuration
	 */
	void setBuilderRegistry(DockerRegistry builderRegistry) {
		this.builderRegistry = builderRegistry;
	}

	/**
	 * Configuration of the Docker registry where the generated image will be published.
	 * @return the registry configuration
	 */
	DockerRegistry getPublishRegistry() {
		return this.publishRegistry;
	}

	/**
	 * Sets the {@link DockerRegistry} that configures authentication to the publishing
	 * registry.
	 * @param builderRegistry the registry configuration
	 */
	void setPublishRegistry(DockerRegistry builderRegistry) {
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

	private DockerRegistryAuthentication getRegistryAuthentication(String type, DockerRegistry registry,
			DockerRegistryAuthentication fallback) {
		if (registry == null || registry.isEmpty()) {
			return fallback;
		}
		if (registry.hasTokenAuth() && !registry.hasUserAuth()) {
			return DockerRegistryAuthentication.token(registry.getToken());
		}
		if (registry.hasUserAuth() && !registry.hasTokenAuth()) {
			return DockerRegistryAuthentication.user(registry.getUsername(), registry.getPassword(), registry.getUrl(),
					registry.getEmail());
		}
		throw new IllegalArgumentException("Invalid Docker " + type
				+ " registry configuration, either token or username/password must be provided");
	}

	/**
	 * Encapsulates Docker registry authentication configuration options.
	 */
	public static class DockerRegistry {

		private String username;

		private String password;

		private String url;

		private String email;

		private String token;

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
		public String getUsername() {
			return this.username;
		}

		void setUsername(String username) {
			this.username = username;
		}

		/**
		 * The password that will be used for user authentication to the registry.
		 * @return the password
		 */
		public String getPassword() {
			return this.password;
		}

		void setPassword(String password) {
			this.password = password;
		}

		/**
		 * The email address that will be used for user authentication to the registry.
		 * @return the email address
		 */
		public String getEmail() {
			return this.email;
		}

		void setEmail(String email) {
			this.email = email;
		}

		/**
		 * The URL of the registry.
		 * @return the registry URL
		 */
		String getUrl() {
			return this.url;
		}

		void setUrl(String url) {
			this.url = url;
		}

		/**
		 * The token that will be used for token authentication to the registry.
		 * @return the authentication token
		 */
		public String getToken() {
			return this.token;
		}

		void setToken(String token) {
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
