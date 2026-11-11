/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.cloudfoundry.autoconfigure.actuate.endpoint;

import org.jspecify.annotations.Nullable;

import org.springframework.http.HttpStatus;

/**
 * Authorization exceptions thrown to limit access to the endpoints.
 *
 * @author Madhura Bhave
 * @since 4.0.0
 */
public class CloudFoundryAuthorizationException extends RuntimeException {

	private final Reason reason;

	public CloudFoundryAuthorizationException(Reason reason, @Nullable String message) {
		this(reason, message, null);
	}

	public CloudFoundryAuthorizationException(Reason reason, @Nullable String message, @Nullable Throwable cause) {
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
