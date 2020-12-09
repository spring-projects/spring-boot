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
 * Authorization exceptions thrown to limit access to the endpoints.
 *
 * @author Madhura Bhave
 * @since 2.0.0
 */
public class CloudFoundryAuthorizationException extends RuntimeException {

	private final Reason reason;

	public CloudFoundryAuthorizationException(Reason reason, String message) {
		this(reason, message, null);
	}

	public CloudFoundryAuthorizationException(Reason reason, String message, Throwable cause) {
		super(message, cause);
		this.reason = reason;
	}

	/**
	 * Return the status code that should be returned to the client.
	 * @return the HTTP status code
	 */
	public HttpStatus getStatusCode() {
		return getReason().getStatus();
	}

	/**
	 * Return the reason why the authorization exception was thrown.
	 * @return the reason
	 */
	public Reason getReason() {
		return this.reason;
	}

	/**
	 * Reasons why the exception can be thrown.
	 */
	public enum Reason {

		/**
		 * Access Denied.
		 */
		ACCESS_DENIED(HttpStatus.FORBIDDEN),

		/**
		 * Invalid Audience.
		 */
		INVALID_AUDIENCE(HttpStatus.UNAUTHORIZED),

		/**
		 * Invalid Issuer.
		 */
		INVALID_ISSUER(HttpStatus.UNAUTHORIZED),

		/**
		 * Invalid Key ID.
		 */
		INVALID_KEY_ID(HttpStatus.UNAUTHORIZED),

		/**
		 * Invalid Signature.
		 */
		INVALID_SIGNATURE(HttpStatus.UNAUTHORIZED),

		/**
		 * Invalid Token.
		 */
		INVALID_TOKEN(HttpStatus.UNAUTHORIZED),

		/**
		 * Missing Authorization.
		 */
		MISSING_AUTHORIZATION(HttpStatus.UNAUTHORIZED),

		/**
		 * Token Expired.
		 */
		TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED),

		/**
		 * Unsupported Token Signing Algorithm.
		 */
		UNSUPPORTED_TOKEN_SIGNING_ALGORITHM(HttpStatus.UNAUTHORIZED),

		/**
		 * Service Unavailable.
		 */
		SERVICE_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE);

		private final HttpStatus status;

		Reason(HttpStatus status) {
			this.status = status;
		}

		public HttpStatus getStatus() {
			return this.status;
		}

	}

}
