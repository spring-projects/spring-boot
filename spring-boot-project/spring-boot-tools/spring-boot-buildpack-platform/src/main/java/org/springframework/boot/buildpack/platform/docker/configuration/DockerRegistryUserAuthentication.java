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

package org.springframework.boot.buildpack.platform.docker.configuration;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Docker registry authentication configuration using user credentials.
 *
 * @author Scott Frederick
 */
class DockerRegistryUserAuthentication extends JsonEncodedDockerRegistryAuthentication {

	@JsonProperty
	private final String username;

	@JsonProperty
	private final String password;

	@JsonProperty("serveraddress")
	private final String url;

	@JsonProperty
	private final String email;

	/**
	 * Constructs a new DockerRegistryUserAuthentication object with the specified
	 * username, password, url, and email.
	 * @param username the username for authentication
	 * @param password the password for authentication
	 * @param url the URL of the Docker registry
	 * @param email the email associated with the user
	 */
	DockerRegistryUserAuthentication(String username, String password, String url, String email) {
		this.username = username;
		this.password = password;
		this.url = url;
		this.email = email;
		createAuthHeader();
	}

	/**
	 * Returns the username associated with this DockerRegistryUserAuthentication
	 * instance.
	 * @return the username
	 */
	String getUsername() {
		return this.username;
	}

	/**
	 * Returns the password of the DockerRegistryUserAuthentication.
	 * @return the password of the DockerRegistryUserAuthentication
	 */
	String getPassword() {
		return this.password;
	}

	/**
	 * Returns the URL of the Docker registry.
	 * @return the URL of the Docker registry
	 */
	String getUrl() {
		return this.url;
	}

	/**
	 * Returns the email associated with the DockerRegistryUserAuthentication object.
	 * @return the email associated with the DockerRegistryUserAuthentication object
	 */
	String getEmail() {
		return this.email;
	}

}
