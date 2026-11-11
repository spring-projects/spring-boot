/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.buildpack.platform.docker.configuration;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * {@link DockerRegistryAuthentication} for
 * {@link DockerRegistryAuthentication#user(String, String, String, String)}.
 *
 * @author Scott Frederick
 */
class DockerRegistryTokenAuthentication extends JsonEncodedDockerRegistryAuthentication {

	@JsonProperty("identitytoken")
	private final String token;

	DockerRegistryTokenAuthentication(String token) {
		this.token = token;
		createAuthHeader();
	}

	String getToken() {
		return this.token;
	}

}
