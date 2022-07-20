/*
 * Copyright 2012-2022 the original author or authors.
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

package org.springframework.boot.gradle.tasks.bundling;

import org.gradle.api.Action;
import org.gradle.api.GradleException;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Nested;
import org.gradle.api.tasks.Optional;

import org.springframework.boot.buildpack.platform.docker.configuration.DockerConfiguration;

/**
 * Encapsulates Docker configuration options.
 *
 * @author Wei Jiang
 * @author Scott Frederick
 * @since 2.4.0
 */
public class DockerSpec {

	private String host;

	private boolean tlsVerify;

	private String certPath;

	private boolean bindHostToBuilder;

	private final DockerRegistrySpec builderRegistry;

	private final DockerRegistrySpec publishRegistry;

	public DockerSpec() {
		this.builderRegistry = new DockerRegistrySpec();
		this.publishRegistry = new DockerRegistrySpec();
	}

	DockerSpec(DockerRegistrySpec builderRegistry, DockerRegistrySpec publishRegistry) {
		this.builderRegistry = builderRegistry;
		this.publishRegistry = publishRegistry;
	}

	@Input
	@Optional
	public String getHost() {
		return this.host;
	}

	public void setHost(String host) {
		this.host = host;
	}

	@Input
	@Optional
	public Boolean isTlsVerify() {
		return this.tlsVerify;
	}

	public void setTlsVerify(boolean tlsVerify) {
		this.tlsVerify = tlsVerify;
	}

	@Input
	@Optional
	public String getCertPath() {
		return this.certPath;
	}

	public void setCertPath(String certPath) {
		this.certPath = certPath;
	}

	@Input
	@Optional
	public Boolean isBindHostToBuilder() {
		return this.bindHostToBuilder;
	}

	public void setBindHostToBuilder(boolean use) {
		this.bindHostToBuilder = use;
	}

	/**
	 * Returns the {@link DockerRegistrySpec} that configures authentication to the
	 * builder registry.
	 * @return the registry spec
	 */
	@Nested
	public DockerRegistrySpec getBuilderRegistry() {
		return this.builderRegistry;
	}

	/**
	 * Customizes the {@link DockerRegistrySpec} that configures authentication to the
	 * builder registry.
	 * @param action the action to apply
	 */
	public void builderRegistry(Action<DockerRegistrySpec> action) {
		action.execute(this.builderRegistry);
	}

	/**
	 * Returns the {@link DockerRegistrySpec} that configures authentication to the
	 * publishing registry.
	 * @return the registry spec
	 */
	@Nested
	public DockerRegistrySpec getPublishRegistry() {
		return this.publishRegistry;
	}

	/**
	 * Customizes the {@link DockerRegistrySpec} that configures authentication to the
	 * publishing registry.
	 * @param action the action to apply
	 */
	public void publishRegistry(Action<DockerRegistrySpec> action) {
		action.execute(this.publishRegistry);
	}

	/**
	 * Returns this configuration as a {@link DockerConfiguration} instance. This method
	 * should only be called when the configuration is complete and will no longer be
	 * changed.
	 * @return the Docker configuration
	 */
	DockerConfiguration asDockerConfiguration() {
		DockerConfiguration dockerConfiguration = new DockerConfiguration();
		dockerConfiguration = customizeHost(dockerConfiguration);
		dockerConfiguration = dockerConfiguration.withBindHostToBuilder(this.bindHostToBuilder);
		dockerConfiguration = customizeBuilderAuthentication(dockerConfiguration);
		dockerConfiguration = customizePublishAuthentication(dockerConfiguration);
		return dockerConfiguration;
	}

	private DockerConfiguration customizeHost(DockerConfiguration dockerConfiguration) {
		if (this.host != null) {
			return dockerConfiguration.withHost(this.host, this.tlsVerify, this.certPath);
		}
		return dockerConfiguration;
	}

	private DockerConfiguration customizeBuilderAuthentication(DockerConfiguration dockerConfiguration) {
		if (this.builderRegistry == null || this.builderRegistry.hasEmptyAuth()) {
			return dockerConfiguration;
		}
		if (this.builderRegistry.hasTokenAuth() && !this.builderRegistry.hasUserAuth()) {
			return dockerConfiguration.withBuilderRegistryTokenAuthentication(this.builderRegistry.getToken());
		}
		if (this.builderRegistry.hasUserAuth() && !this.builderRegistry.hasTokenAuth()) {
			return dockerConfiguration.withBuilderRegistryUserAuthentication(this.builderRegistry.getUsername(),
					this.builderRegistry.getPassword(), this.builderRegistry.getUrl(), this.builderRegistry.getEmail());
		}
		throw new GradleException(
				"Invalid Docker builder registry configuration, either token or username/password must be provided");
	}

	private DockerConfiguration customizePublishAuthentication(DockerConfiguration dockerConfiguration) {
		if (this.publishRegistry == null || this.publishRegistry.hasEmptyAuth()) {
			return dockerConfiguration.withEmptyPublishRegistryAuthentication();
		}
		if (this.publishRegistry.hasTokenAuth() && !this.publishRegistry.hasUserAuth()) {
			return dockerConfiguration.withPublishRegistryTokenAuthentication(this.publishRegistry.getToken());
		}
		if (this.publishRegistry.hasUserAuth() && !this.publishRegistry.hasTokenAuth()) {
			return dockerConfiguration.withPublishRegistryUserAuthentication(this.publishRegistry.getUsername(),
					this.publishRegistry.getPassword(), this.publishRegistry.getUrl(), this.publishRegistry.getEmail());
		}
		throw new GradleException(
				"Invalid Docker publish registry configuration, either token or username/password must be provided");
	}

	/**
	 * Encapsulates Docker registry authentication configuration options.
	 */
	public static class DockerRegistrySpec {

		private String username;

		private String password;

		private String url;

		private String email;

		private String token;

		public DockerRegistrySpec() {
		}

		DockerRegistrySpec(String username, String password, String url, String email) {
			this.username = username;
			this.password = password;
			this.url = url;
			this.email = email;
		}

		DockerRegistrySpec(String token) {
			this.token = token;
		}

		/**
		 * Returns the username to use when authenticating to the Docker registry.
		 * @return the registry username
		 */
		@Input
		@Optional
		public String getUsername() {
			return this.username;
		}

		/**
		 * Sets the username to use when authenticating to the Docker registry.
		 * @param username the registry username
		 */
		public void setUsername(String username) {
			this.username = username;
		}

		/**
		 * Returns the password to use when authenticating to the Docker registry.
		 * @return the registry password
		 */
		@Input
		@Optional
		public String getPassword() {
			return this.password;
		}

		/**
		 * Sets the password to use when authenticating to the Docker registry.
		 * @param password the registry username
		 */
		public void setPassword(String password) {
			this.password = password;
		}

		/**
		 * Returns the Docker registry URL.
		 * @return the registry URL
		 */
		@Input
		@Optional
		public String getUrl() {
			return this.url;
		}

		/**
		 * Sets the Docker registry URL.
		 * @param url the registry URL
		 */
		public void setUrl(String url) {
			this.url = url;
		}

		/**
		 * Returns the email address associated with the Docker registry username.
		 * @return the registry email address
		 */
		@Input
		@Optional
		public String getEmail() {
			return this.email;
		}

		/**
		 * Sets the email address associated with the Docker registry username.
		 * @param email the registry email address
		 */
		public void setEmail(String email) {
			this.email = email;
		}

		/**
		 * Returns the identity token to use when authenticating to the Docker registry.
		 * @return the registry identity token
		 */
		@Input
		@Optional
		public String getToken() {
			return this.token;
		}

		/**
		 * Sets the identity token to use when authenticating to the Docker registry.
		 * @param token the registry identity token
		 */
		public void setToken(String token) {
			this.token = token;
		}

		boolean hasEmptyAuth() {
			return this.username == null && this.password == null && this.url == null && this.email == null
					&& this.token == null;
		}

		boolean hasUserAuth() {
			return this.getUsername() != null && this.getPassword() != null;
		}

		boolean hasTokenAuth() {
			return this.getToken() != null;
		}

	}

}
