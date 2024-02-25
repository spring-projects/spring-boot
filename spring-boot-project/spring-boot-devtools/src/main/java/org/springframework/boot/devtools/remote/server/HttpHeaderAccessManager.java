/*
 * Copyright 2012-2021 the original author or authors.
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

	private final String expectedSecret;

	/**
     * Constructs a new HttpHeaderAccessManager with the specified header name and expected secret.
     * 
     * @param headerName the name of the header to be checked
     * @param expectedSecret the expected secret value for the header
     * @throws IllegalArgumentException if the headerName or expectedSecret is empty
     */
    public HttpHeaderAccessManager(String headerName, String expectedSecret) {
		Assert.hasLength(headerName, "HeaderName must not be empty");
		Assert.hasLength(expectedSecret, "ExpectedSecret must not be empty");
		this.headerName = headerName;
		this.expectedSecret = expectedSecret;
	}

	/**
     * Checks if the provided secret in the request header matches the expected secret.
     * 
     * @param request the server HTTP request
     * @return true if the provided secret matches the expected secret, false otherwise
     */
    @Override
	public boolean isAllowed(ServerHttpRequest request) {
		String providedSecret = request.getHeaders().getFirst(this.headerName);
		return this.expectedSecret.equals(providedSecret);
	}

}
