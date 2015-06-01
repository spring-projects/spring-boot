/*
 * Copyright 2012-2015 the original author or authors.
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

package org.springframework.boot.developertools.remote.server;

import java.io.IOException;

import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.util.Assert;

/**
 * {@link Handler} that responds with a specific {@link HttpStatus}.
 *
 * @author Phillip Webb
 */
public class HttpStatusHandler implements Handler {

	private final HttpStatus status;

	/**
	 * Create a new {@link HttpStatusHandler} instance that will respond with a HTTP OK 200
	 * status.
	 */
	public HttpStatusHandler() {
		this(HttpStatus.OK);
	}

	/**
	 * Create a new {@link HttpStatusHandler} instance that will respond with the specified
	 * status.
	 * @param status the status
	 */
	public HttpStatusHandler(HttpStatus status) {
		Assert.notNull(status, "Status must not be null");
		this.status = status;
	}

	@Override
	public void handle(ServerHttpRequest request, ServerHttpResponse response)
			throws IOException {
		response.setStatusCode(this.status);
	}

}
