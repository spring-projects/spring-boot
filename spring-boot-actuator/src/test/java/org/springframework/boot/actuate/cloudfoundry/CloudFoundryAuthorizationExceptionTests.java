/*
 * Copyright 2012-2016 the original author or authors.
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

package org.springframework.boot.actuate.cloudfoundry;

import org.junit.Test;

import org.springframework.boot.actuate.cloudfoundry.CloudFoundryAuthorizationException.Reason;
import org.springframework.http.HttpStatus;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link CloudFoundryAuthorizationException}.
 *
 * @author Madhura Bhave
 */
public class CloudFoundryAuthorizationExceptionTests {

	@Test
	public void statusCodeForInvalidTokenReasonShouldBe401() throws Exception {
		assertThat(createException(Reason.INVALID_TOKEN).getStatusCode())
				.isEqualTo(HttpStatus.UNAUTHORIZED);
	}

	@Test
	public void statusCodeForInvalidIssuerReasonShouldBe401() throws Exception {
		assertThat(createException(Reason.INVALID_ISSUER).getStatusCode())
				.isEqualTo(HttpStatus.UNAUTHORIZED);
	}

	@Test
	public void statusCodeForInvalidAudienceReasonShouldBe401() throws Exception {
		assertThat(createException(Reason.INVALID_AUDIENCE).getStatusCode())
				.isEqualTo(HttpStatus.UNAUTHORIZED);
	}

	@Test
	public void statusCodeForInvalidSignatureReasonShouldBe401() throws Exception {
		assertThat(createException(Reason.INVALID_SIGNATURE).getStatusCode())
				.isEqualTo(HttpStatus.UNAUTHORIZED);
	}

	@Test
	public void statusCodeForMissingAuthorizationReasonShouldBe401() throws Exception {
		assertThat(createException(Reason.MISSING_AUTHORIZATION).getStatusCode())
				.isEqualTo(HttpStatus.UNAUTHORIZED);
	}

	@Test
	public void statusCodeForUnsupportedSignatureAlgorithmReasonShouldBe401()
			throws Exception {
		assertThat(createException(Reason.UNSUPPORTED_TOKEN_SIGNING_ALGORITHM)
				.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
	}

	@Test
	public void statusCodeForTokenExpiredReasonShouldBe401() throws Exception {
		assertThat(createException(Reason.TOKEN_EXPIRED).getStatusCode())
				.isEqualTo(HttpStatus.UNAUTHORIZED);
	}

	@Test
	public void statusCodeForAccessDeniedReasonShouldBe403() throws Exception {
		assertThat(createException(Reason.ACCESS_DENIED).getStatusCode())
				.isEqualTo(HttpStatus.FORBIDDEN);
	}

	@Test
	public void statusCodeForServiceUnavailableReasonShouldBe503() throws Exception {
		assertThat(createException(Reason.SERVICE_UNAVAILABLE).getStatusCode())
				.isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
	}

	private CloudFoundryAuthorizationException createException(Reason reason) {
		return new CloudFoundryAuthorizationException(reason, "message");
	}

}
