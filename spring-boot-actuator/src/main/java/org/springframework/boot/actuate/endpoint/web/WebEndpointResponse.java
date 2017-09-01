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

package org.springframework.boot.actuate.endpoint.web;

import org.springframework.boot.actuate.endpoint.web.annotation.WebEndpointExtension;

/**
 * A {@code WebEndpointResponse} can be returned by an operation on a
 * {@link WebEndpointExtension} to provide additional, web-specific information such as
 * the HTTP status code.
 *
 * @param <T> the type of the response body
 * @author Stephane Nicoll
 * @author Andy Wilkinson
 * @since 2.0.0
 */
public final class WebEndpointResponse<T> {

	private final T body;

	private final int status;

	/**
	 * Creates a new {@code WebEndpointResponse} with no body and a 200 (OK) status.
	 */
	public WebEndpointResponse() {
		this(null);
	}

	/**
	 * Creates a new {@code WebEndpointResponse} with no body and the given
	 * {@code status}.
	 * @param status the HTTP status
	 */
	public WebEndpointResponse(int status) {
		this(null, status);
	}

	/**
	 * Creates a new {@code WebEndpointResponse} with then given body and a 200 (OK)
	 * status.
	 * @param body the body
	 */
	public WebEndpointResponse(T body) {
		this(body, 200);
	}

	/**
	 * Creates a new {@code WebEndpointResponse} with then given body and status.
	 * @param body the body
	 * @param status the HTTP status
	 */
	public WebEndpointResponse(T body, int status) {
		this.body = body;
		this.status = status;
	}

	/**
	 * Returns the body for the response.
	 * @return the body
	 */
	public T getBody() {
		return this.body;
	}

	/**
	 * Returns the status for the response.
	 * @return the status
	 */
	public int getStatus() {
		return this.status;
	}

}
