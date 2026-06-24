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

package org.springframework.boot.security.oauth2.server.resource.autoconfigure;

import org.jspecify.annotations.Nullable;

import org.springframework.core.env.Environment;
import org.springframework.util.StringUtils;

/**
 * Property names for the configured JWK Set URI.
 *
 * @author Hyeonseok Lee
 */
final class JwkSetUriProperty {

	static final String NAME = "spring.security.oauth2.resourceserver.jwt.jwkset.uri";

	static final String DEPRECATED_NAME = "spring.security.oauth2.resourceserver.jwt.jwk-set-uri";

	private JwkSetUriProperty() {
	}

	static @Nullable String get(Environment environment) {
		String jwkSetUri = environment.getProperty(NAME);
		if (StringUtils.hasText(jwkSetUri)) {
			return jwkSetUri;
		}
		return environment.getProperty(DEPRECATED_NAME);
	}

}
