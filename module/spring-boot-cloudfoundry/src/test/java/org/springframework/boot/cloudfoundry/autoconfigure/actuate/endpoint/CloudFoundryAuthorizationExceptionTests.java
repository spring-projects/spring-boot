/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.cloudfoundry.autoconfigure.actuate.endpoint;

import org.junit.jupiter.api.Test;

import org.springframework.boot.cloudfoundry.autoconfigure.actuate.endpoint.CloudFoundryAuthorizationException.Reason;
import org.springframework.http.HttpStatus;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link CloudFoundryAuthorizationException}.
 *
 * @author Madhura Bhave
 */
class CloudFoundryAuthorizationExceptionTests {

	@Test
	void statusCodeForInvalidTokenReasonShouldBe401() {
		assertThat(createException(Reason.INVALID_TOKEN).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
	}

	@Test
	void statusCodeForInvalidIssuerReasonShouldBe401() {
		assertThat(createException(Reason.INVALID_ISSUER).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
	}

	@Test
	void statusCodeForInvalidAudienceReasonShouldBe401() {
		assertThat(createException(Reason.INVALID_AUDIENCE).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
	}

	@Test
	void statusCodeForInvalidSignatureReasonShouldBe401() {
		assertThat(createException(Reason.INVALID_SIGNATURE).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
	}

	@Test
	void statusCodeForMissingAuthorizationReasonShouldBe401() {
		assertThat(createException(Reason.MISSING_AUTHORIZATION).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
	}

	@Test
	void statusCodeForUnsupportedSignatureAlgorithmReasonShouldBe401() {
		assertThat(createException(Reason.UNSUPPORTED_TOKEN_SIGNING_ALGORITHM).getStatusCode())
			.isEqualTo(HttpStatus.UNAUTHORIZED);
	}

	@Test
	void statusCodeForTokenExpiredReasonShouldBe401() {
		assertThat(createException(Reason.TOKEN_EXPIRED).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
	}

	@Test
	void statusCodeForAccessDeniedReasonShouldBe403() {
		assertThat(createException(Reason.ACCESS_DENIED).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
	}

	@Test
	void statusCodeForServiceUnavailableReasonShouldBe503() {
		assertThat(createException(Reason.SERVICE_UNAVAILABLE).getStatusCode())
			.isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
	}

	private CloudFoundryAuthorizationException createException(Reason reason) {
		return new CloudFoundryAuthorizationException(reason, "message");
	}

}
