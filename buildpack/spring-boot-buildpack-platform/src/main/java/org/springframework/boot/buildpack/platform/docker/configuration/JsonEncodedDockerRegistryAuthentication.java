/*
 * Copyright 2012-present the original author or authors.
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
