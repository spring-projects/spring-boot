/*
 * Copyright 2012-2020 the original author or authors.
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

import org.springframework.boot.buildpack.platform.docker.configuration.DockerConfiguration;

/**
 * Docker configuration options.
 *
 * @author Wei Jiang
 * @author Scott Frederick
 * @since 2.4.0
 */
public class Docker {

	private String host;

	private boolean tlsVerify;

	private String certPath;

	private DockerRegistry builderRegistry;

	private DockerRegistry publishRegistry;

	public String getHost() {
		return this.host;
	}

	public void setHost(String host) {
		this.host = host;
	}

	public boolean isTlsVerify() {
		return this.tlsVerify;
	}

	public void setTlsVerify(boolean tlsVerify) {
		this.tlsVerify = tlsVerify;
	}

	public String getCertPath() {
		return this.certPath;
	}

	public void setCertPath(String certPath) {
		this.certPath = certPath;
	}

	DockerRegistry getBuilderRegistry() {
		return this.builderRegistry;
	}

	/**
	 * Sets the {@link DockerRegistry} that configures authentication to the builder
	 * registry.
	 * @param builderRegistry the registry configuration
	 */
	public void setBuilderRegistry(DockerRegistry builderRegistry) {
		this.builderRegistry = builderRegistry;
	}

	DockerRegistry getPublishRegistry() {
		return this.publishRegistry;
	}

	/**
	 * Sets the {@link DockerRegistry} that configures authentication to the publishing
	 * registry.
	 * @param builderRegistry the registry configuration
	 */
	public void setPublishRegistry(DockerRegistry builderRegistry) {
		this.publishRegistry = builderRegistry;
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
		if (this.builderRegistry == null || this.builderRegistry.isEmpty()) {
			return dockerConfiguration;
		}
		if (this.builderRegistry.hasTokenAuth() && !this.builderRegistry.hasUserAuth()) {
			return dockerConfiguration.withBuilderRegistryTokenAuthentication(this.builderRegistry.getToken());
		}
		if (this.builderRegistry.hasUserAuth() && !this.builderRegistry.hasTokenAuth()) {
			return dockerConfiguration.withBuilderRegistryUserAuthentication(this.builderRegistry.getUsername(),
					this.builderRegistry.getPassword(), this.builderRegistry.getUrl(), this.builderRegistry.getEmail());
		}
		throw new IllegalArgumentException(
				"Invalid Docker builder registry configuration, either token or username/password must be provided");
	}

	private DockerConfiguration customizePublishAuthentication(DockerConfiguration dockerConfiguration) {
		if (this.publishRegistry == null || this.publishRegistry.isEmpty()) {
			return dockerConfiguration;
		}
		if (this.publishRegistry.hasTokenAuth() && !this.publishRegistry.hasUserAuth()) {
			return dockerConfiguration.withPublishRegistryTokenAuthentication(this.publishRegistry.getToken());
		}
		if (this.publishRegistry.hasUserAuth() && !this.publishRegistry.hasTokenAuth()) {
			return dockerConfiguration.withPublishRegistryUserAuthentication(this.publishRegistry.getUsername(),
					this.publishRegistry.getPassword(), this.publishRegistry.getUrl(), this.publishRegistry.getEmail());
		}
		throw new IllegalArgumentException(
				"Invalid Docker publish registry configuration, either token or username/password must be provided");
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

		String getUsername() {
			return this.username;
		}

		public void setUsername(String username) {
			this.username = username;
		}

		String getPassword() {
			return this.password;
		}

		public void setPassword(String password) {
			this.password = password;
		}

		String getEmail() {
			return this.email;
		}

		public void setEmail(String email) {
			this.email = email;
		}

		String getUrl() {
			return this.url;
		}

		public void setUrl(String url) {
			this.url = url;
		}

		String getToken() {
			return this.token;
		}

		public void setToken(String token) {
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
