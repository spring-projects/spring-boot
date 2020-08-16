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

package org.springframework.boot.gradle.tasks.bundling;

import org.springframework.boot.buildpack.platform.docker.configuration.DockerRegistryConfiguration;

/**
 * Docker registry configuration options.
 *
 * @author Wei Jiang
 * @since 2.4.0
 */
public class DockerRegistry {

	/**
	 * Docker registry server address.
	 */
	private String url;

	/**
	 * Docker registry authentication username.
	 */
	private String username;

	/**
	 * Docker registry authentication password.
	 */
	private String password;

	/**
	 * Docker registry authentication email.
	 */
	private String email;

	/**
	 * Docker registry authentication identity token.
	 */
	private String token;

	public String getUrl() {
		return this.url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public String getUsername() {
		return this.username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getPassword() {
		return this.password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public String getEmail() {
		return this.email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public String getToken() {
		return this.token;
	}

	public void setToken(String token) {
		this.token = token;
	}

	public DockerRegistryConfiguration getDockerRegistryConfiguration() {
		return new DockerRegistryConfiguration(this.url, this.username, this.password, this.email, this.token);
	}

}
