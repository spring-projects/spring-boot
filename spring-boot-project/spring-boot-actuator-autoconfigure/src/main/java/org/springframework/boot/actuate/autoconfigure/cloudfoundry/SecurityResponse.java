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

package org.springframework.boot.actuate.autoconfigure.cloudfoundry;

import org.springframework.http.HttpStatus;

/**
 * Response from the Cloud Foundry security interceptors.
 *
 * @author Madhura Bhave
 * @since 2.0.0
 */
public class SecurityResponse {

	private final HttpStatus status;

	private final String message;

	/**
	 * Constructs a new SecurityResponse object with the specified HttpStatus status.
	 * @param status the HttpStatus status to be set for the SecurityResponse object
	 */
	public SecurityResponse(HttpStatus status) {
		this(status, null);
	}

	/**
	 * Constructs a new SecurityResponse object with the specified HTTP status and
	 * message.
	 * @param status the HTTP status of the response
	 * @param message the message associated with the response
	 */
	public SecurityResponse(HttpStatus status, String message) {
		this.status = status;
		this.message = message;
	}

	/**
	 * Returns the HTTP status of the security response.
	 * @return the HTTP status of the security response
	 */
	public HttpStatus getStatus() {
		return this.status;
	}

	/**
	 * Returns the message of the SecurityResponse object.
	 * @return the message of the SecurityResponse object
	 */
	public String getMessage() {
		return this.message;
	}

	/**
	 * Creates a new SecurityResponse object with a success status code.
	 * @return a new SecurityResponse object with a success status code
	 */
	public static SecurityResponse success() {
		return new SecurityResponse(HttpStatus.OK);
	}

}
