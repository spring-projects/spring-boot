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

	DockerRegistryUserAuthentication(String username, String password, String url, String email) {
		this.username = username;
		this.password = password;
		this.url = url;
		this.email = email;
		createAuthHeader();
	}

	String getUsername() {
		return this.username;
	}

	String getPassword() {
		return this.password;
	}

	String getUrl() {
		return this.url;
	}

	String getEmail() {
		return this.email;
	}

}
