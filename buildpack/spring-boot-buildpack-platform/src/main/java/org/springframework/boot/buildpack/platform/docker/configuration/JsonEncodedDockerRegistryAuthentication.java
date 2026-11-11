/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.buildpack.platform.docker.configuration;

import java.util.Base64;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.jspecify.annotations.Nullable;
import tools.jackson.core.JacksonException;

import org.springframework.boot.buildpack.platform.json.SharedJsonMapper;

/**
 * {@link DockerRegistryAuthentication} that uses a Base64 encoded auth header value based
 * on the JSON created from the instance.
 *
 * @author Scott Frederick
 */
class JsonEncodedDockerRegistryAuthentication implements DockerRegistryAuthentication {

	@JsonIgnore
	private @Nullable String authHeader;

	@Override
	public @Nullable String getAuthHeader() {
		return this.authHeader;
	}

	protected void createAuthHeader() {
		try {
			this.authHeader = Base64.getUrlEncoder().encodeToString(SharedJsonMapper.get().writeValueAsBytes(this));
		}
		catch (JacksonException ex) {
			throw new IllegalStateException("Error creating Docker registry authentication header", ex);
		}
	}

}
