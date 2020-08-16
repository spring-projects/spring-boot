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

import java.io.IOException;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import org.springframework.boot.buildpack.platform.json.SharedObjectMapper;
import org.springframework.util.Base64Utils;
import org.springframework.util.StringUtils;

/**
 * Docker registry configuration options.
 *
 * @author Wei Jiang
 * @since 2.4.0
 */
public class DockerRegistryConfiguration {

	/**
	 * Docker registry server address.
	 */
	@JsonProperty("serveraddress")
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
	@JsonIgnore
	private String token;

	public DockerRegistryConfiguration() {
		super();
	}

	public DockerRegistryConfiguration(String url, String username, String password, String email, String token) {
		super();
		this.url = url;
		this.username = username;
		this.password = password;
		this.email = email;
		this.token = token;
	}

	public String createDockerRegistryAuthToken() {
		if (!StringUtils.isEmpty(this.getToken())) {
			return this.getToken();
		}

		try {
			return Base64Utils.encodeToString(SharedObjectMapper.get().writeValueAsBytes(this));
		}
		catch (IOException ex) {
			throw new IllegalStateException("create docker registry authentication token failed.", ex);
		}
	}

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

}
