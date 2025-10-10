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

package org.springframework.boot.cloudfoundry.actuate.autoconfigure.endpoint.servlet;

import java.util.Base64;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.boot.actuate.endpoint.EndpointId;
import org.springframework.boot.cloudfoundry.actuate.autoconfigure.endpoint.AccessLevel;
import org.springframework.boot.cloudfoundry.actuate.autoconfigure.endpoint.CloudFoundryAuthorizationException.Reason;
import org.springframework.boot.cloudfoundry.actuate.autoconfigure.endpoint.SecurityResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.assertArg;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

/**
 * Tests for {@link SecurityInterceptor}.
 *
 * @author Madhura Bhave
 */
@ExtendWith(MockitoExtension.class)
class SecurityInterceptorTests {

	@Mock
	@SuppressWarnings("NullAway.Init")
	private TokenValidator tokenValidator;

	@Mock
	@SuppressWarnings("NullAway.Init")
	private SecurityService securityService;

	private SecurityInterceptor interceptor;

	private MockHttpServletRequest request;

	@BeforeEach
	void setup() {
		this.interceptor = new SecurityInterceptor(this.tokenValidator, this.securityService, "my-app-id");
		this.request = new MockHttpServletRequest();
	}

	@Test
	void preHandleWhenRequestIsPreFlightShouldReturnTrue() {
		this.request.setMethod("OPTIONS");
		this.request.addHeader(HttpHeaders.ORIGIN, "https://example.com");
		this.request.addHeader(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET");
		SecurityResponse response = this.interceptor.preHandle(this.request, EndpointId.of("test"));
		assertThat(response.getStatus()).isEqualTo(HttpStatus.OK);
	}

	@Test
	void preHandleWhenTokenIsMissingShouldReturnFalse() {
		SecurityResponse response = this.interceptor.preHandle(this.request, EndpointId.of("test"));
		assertThat(response.getStatus()).isEqualTo(Reason.MISSING_AUTHORIZATION.getStatus());
	}

	@Test
	void preHandleWhenTokenIsNotBearerShouldReturnFalse() {
		this.request.addHeader("Authorization", mockAccessToken());
		SecurityResponse response = this.interceptor.preHandle(this.request, EndpointId.of("test"));
		assertThat(response.getStatus()).isEqualTo(Reason.MISSING_AUTHORIZATION.getStatus());
	}

	@Test
	void preHandleWhenApplicationIdIsNullShouldReturnFalse() {
		this.interceptor = new SecurityInterceptor(this.tokenValidator, this.securityService, null);
		this.request.addHeader("Authorization", "bearer " + mockAccessToken());
		SecurityResponse response = this.interceptor.preHandle(this.request, EndpointId.of("test"));
		assertThat(response.getStatus()).isEqualTo(Reason.SERVICE_UNAVAILABLE.getStatus());
	}

	@Test
	void preHandleWhenCloudFoundrySecurityServiceIsNullShouldReturnFalse() {
		this.interceptor = new SecurityInterceptor(this.tokenValidator, null, "my-app-id");
		this.request.addHeader("Authorization", "bearer " + mockAccessToken());
		SecurityResponse response = this.interceptor.preHandle(this.request, EndpointId.of("test"));
		assertThat(response.getStatus()).isEqualTo(Reason.SERVICE_UNAVAILABLE.getStatus());
	}

	@Test
	void preHandleWhenAccessIsNotAllowedShouldReturnFalse() {
		String accessToken = mockAccessToken();
		this.request.addHeader("Authorization", "bearer " + accessToken);
		given(this.securityService.getAccessLevel(accessToken, "my-app-id")).willReturn(AccessLevel.RESTRICTED);
		SecurityResponse response = this.interceptor.preHandle(this.request, EndpointId.of("test"));
		assertThat(response.getStatus()).isEqualTo(Reason.ACCESS_DENIED.getStatus());
	}

	@Test
	void preHandleSuccessfulWithFullAccess() {
		String accessToken = mockAccessToken();
		this.request.addHeader("Authorization", "Bearer " + accessToken);
		given(this.securityService.getAccessLevel(accessToken, "my-app-id")).willReturn(AccessLevel.FULL);
		SecurityResponse response = this.interceptor.preHandle(this.request, EndpointId.of("test"));
		then(this.tokenValidator).should().validate(assertArg((token) -> assertThat(token).hasToString(accessToken)));
		assertThat(response.getStatus()).isEqualTo(HttpStatus.OK);
		assertThat(this.request.getAttribute("cloudFoundryAccessLevel")).isEqualTo(AccessLevel.FULL);
	}

	@Test
	void preHandleSuccessfulWithRestrictedAccess() {
		String accessToken = mockAccessToken();
		this.request.addHeader("Authorization", "Bearer " + accessToken);
		given(this.securityService.getAccessLevel(accessToken, "my-app-id")).willReturn(AccessLevel.RESTRICTED);
		SecurityResponse response = this.interceptor.preHandle(this.request, EndpointId.of("info"));
		then(this.tokenValidator).should().validate(assertArg((token) -> assertThat(token).hasToString(accessToken)));
		assertThat(response.getStatus()).isEqualTo(HttpStatus.OK);
		assertThat(this.request.getAttribute("cloudFoundryAccessLevel")).isEqualTo(AccessLevel.RESTRICTED);
	}

	private String mockAccessToken() {
		return "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJ0b3B0YWwu"
				+ "Y29tIiwiZXhwIjoxNDI2NDIwODAwLCJhd2Vzb21lIjp0cnVlfQ."
				+ Base64.getEncoder().encodeToString("signature".getBytes());
	}

}
