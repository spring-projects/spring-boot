/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.buildpack.platform.docker.configuration;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.jspecify.annotations.Nullable;

/**
 * {@link DockerRegistryAuthentication} for
 * {@link DockerRegistryAuthentication#token(String)}.
 *
 * @author Scott Frederick
 */
class DockerRegistryUserAuthentication extends JsonEncodedDockerRegistryAuthentication {

	@JsonProperty
	private final String username;

	@JsonProperty
	private final String password;

	@JsonProperty("serveraddress")
	private final @Nullable String url;

	@JsonProperty
	private final @Nullable String email;

	DockerRegistryUserAuthentication(String username, String password, @Nullable String url, @Nullable String email) {
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

	@Nullable String getUrl() {
		return this.url;
	}

	@Nullable String getEmail() {
		return this.email;
	}

}
