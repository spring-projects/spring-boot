/*
 * Copyright 2012-2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.autoconfigure.security.oauth2.client;

/**
 * OAuth 2.0 authorization grant types supported by Spring Boot.
 *
 * @author Madhura Bhave
 * @author Phillip Webb
 * @since 2.0.0
 */
public enum AuthorizationGrantType {

	/**
	 * An {@code "authorization_code"} grant type.
	 */
	AUTHORIZATION_CODE(
			org.springframework.security.oauth2.core.AuthorizationGrantType.AUTHORIZATION_CODE);

	private final org.springframework.security.oauth2.core.AuthorizationGrantType type;

	AuthorizationGrantType(
			org.springframework.security.oauth2.core.AuthorizationGrantType type) {
		this.type = type;
	}

	org.springframework.security.oauth2.core.AuthorizationGrantType getType() {
		return this.type;
	}

}
