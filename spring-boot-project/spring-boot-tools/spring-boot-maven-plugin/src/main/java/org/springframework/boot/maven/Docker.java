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

	private DockerRegistry registry;

	/**
	 * Sets the {@link DockerRegistry} that configures registry authentication.
	 * @param registry the registry configuration
	 */
	public void setRegistry(DockerRegistry registry) {
		this.registry = registry;
	}

	/**
	 * Returns this configuration as a {@link DockerConfiguration} instance. This method
	 * should only be called when the configuration is complete and will no longer be
	 * changed.
	 * @return the Docker configuration
	 */
	DockerConfiguration asDockerConfiguration() {
		if (this.registry == null || this.registry.isEmpty()) {
			return null;
		}
		if (this.registry.hasTokenAuth() && !this.registry.hasUserAuth()) {
			return DockerConfiguration.withRegistryTokenAuthentication(this.registry.getToken());
		}
		if (this.registry.hasUserAuth() && !this.registry.hasTokenAuth()) {
			return DockerConfiguration.withRegistryUserAuthentication(this.registry.getUsername(),
					this.registry.getPassword(), this.registry.getUrl(), this.registry.getEmail());
		}

		throw new IllegalArgumentException(
				"Invalid Docker registry configuration, either token or username/password must be provided");
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
