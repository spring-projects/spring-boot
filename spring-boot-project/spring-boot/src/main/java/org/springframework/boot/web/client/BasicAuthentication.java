/*
 * Copyright 2012-2019 the original author or authors.
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

package org.springframework.boot.web.client;

import java.nio.charset.Charset;

import org.springframework.util.Assert;

/**
 * Basic authentication properties which are used by
 * {@link BasicAuthenticationClientHttpRequestFactory}.
 *
 * @author Dmytro Nosan
 * @since 2.2.0
 * @see BasicAuthenticationClientHttpRequestFactory
 */
public class BasicAuthentication {

	private final String username;

	private final String password;

	private final Charset charset;

	/**
	 * Create a new {@link BasicAuthentication}.
	 * @param username the username to use
	 * @param password the password to use
	 */
	public BasicAuthentication(String username, String password) {
		this(username, password, null);
	}

	/**
	 * Create a new {@link BasicAuthentication}.
	 * @param username the username to use
	 * @param password the password to use
	 * @param charset the charset to use
	 */
	public BasicAuthentication(String username, String password, Charset charset) {
		Assert.notNull(username, "Username must not be null");
		Assert.notNull(password, "Password must not be null");
		this.username = username;
		this.password = password;
		this.charset = charset;
	}

	/**
	 * The username to use.
	 * @return the username, never {@code null} or {@code empty}.
	 */
	public String getUsername() {
		return this.username;
	}

	/**
	 * The password to use.
	 * @return the password, never {@code null} or {@code empty}.
	 */
	public String getPassword() {
		return this.password;
	}

	/**
	 * The charset to use.
	 * @return the charset, or {@code null}.
	 */
	public Charset getCharset() {
		return this.charset;
	}

}
