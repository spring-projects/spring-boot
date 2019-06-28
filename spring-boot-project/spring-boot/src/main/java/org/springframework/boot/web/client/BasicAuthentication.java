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

import org.springframework.http.HttpHeaders;
import org.springframework.util.Assert;

/**
 * Basic authentication details to be applied to {@link HttpHeaders}.
 *
 * @author Dmytro Nosan
 * @author Ilya Lukyanovich
 */
class BasicAuthentication {

	private final String username;

	private final String password;

	private final Charset charset;

	BasicAuthentication(String username, String password, Charset charset) {
		Assert.notNull(username, "Username must not be null");
		Assert.notNull(password, "Password must not be null");
		this.username = username;
		this.password = password;
		this.charset = charset;
	}

	public void applyTo(HttpHeaders headers) {
		if (!headers.containsKey(HttpHeaders.AUTHORIZATION)) {
			headers.setBasicAuth(this.username, this.password, this.charset);
		}
	}

}
