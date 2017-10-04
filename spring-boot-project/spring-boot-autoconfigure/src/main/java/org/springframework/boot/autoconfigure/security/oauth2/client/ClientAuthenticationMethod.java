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
 * OAuth 2.0 client authentication methods supported by Spring Boot.
 *
 * @author Madhura Bhave
 * @author Phillip Webb
 * @since 2.0.0
 * @see org.springframework.security.oauth2.core.ClientAuthenticationMethod
 */
public enum ClientAuthenticationMethod {

	/**
	 * HTTP BASIC client authentication.
	 */
	BASIC(org.springframework.security.oauth2.core.ClientAuthenticationMethod.BASIC),

	/**
	 * HTTP POST client authentication.
	 */
	POST(org.springframework.security.oauth2.core.ClientAuthenticationMethod.POST);

	private final org.springframework.security.oauth2.core.ClientAuthenticationMethod method;

	ClientAuthenticationMethod(
			org.springframework.security.oauth2.core.ClientAuthenticationMethod method) {
		this.method = method;
	}

	org.springframework.security.oauth2.core.ClientAuthenticationMethod getMethod() {
		return this.method;
	}

}
