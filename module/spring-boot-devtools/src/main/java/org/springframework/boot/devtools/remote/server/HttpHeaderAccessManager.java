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

package org.springframework.boot.devtools.remote.server;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

import org.springframework.http.server.ServerHttpRequest;
import org.springframework.util.Assert;

/**
 * {@link AccessManager} that checks for the presence of an HTTP header secret.
 *
 * @author Rob Winch
 * @author Phillip Webb
 * @since 1.3.0
 */
public class HttpHeaderAccessManager implements AccessManager {

	private final String headerName;

	private final byte[] expectedSecret;

	public HttpHeaderAccessManager(String headerName, String expectedSecret) {
		Assert.hasLength(headerName, "'headerName' must not be empty");
		Assert.hasLength(expectedSecret, "'expectedSecret' must not be empty");
		this.headerName = headerName;
		this.expectedSecret = expectedSecret.getBytes(StandardCharsets.UTF_8);
	}

	@Override
	public boolean isAllowed(ServerHttpRequest request) {
		String providedSecret = request.getHeaders().getFirst(this.headerName);
		return (providedSecret != null)
				&& MessageDigest.isEqual(providedSecret.getBytes(StandardCharsets.UTF_8), this.expectedSecret);
	}

}
