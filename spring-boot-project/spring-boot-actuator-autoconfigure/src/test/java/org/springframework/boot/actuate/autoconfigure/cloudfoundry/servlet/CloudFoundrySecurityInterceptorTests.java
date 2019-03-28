/*
 * Copyright 2012-2018 the original author or authors.
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

package org.springframework.boot.actuate.autoconfigure.cloudfoundry.servlet;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import org.springframework.boot.actuate.autoconfigure.cloudfoundry.AccessLevel;
import org.springframework.boot.actuate.autoconfigure.cloudfoundry.CloudFoundryAuthorizationException.Reason;
import org.springframework.boot.actuate.autoconfigure.cloudfoundry.SecurityResponse;
import org.springframework.boot.actuate.autoconfigure.cloudfoundry.Token;
import org.springframework.boot.actuate.endpoint.EndpointId;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.util.Base64Utils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

/**
 * Tests for {@link CloudFoundrySecurityInterceptor}.
 *
 * @author Madhura Bhave
 */
public class CloudFoundrySecurityInterceptorTests {

	@Mock
	private TokenValidator tokenValidator;

	@Mock
	private CloudFoundrySecurityService securityService;

	private CloudFoundrySecurityInterceptor interceptor;

	private MockHttpServletRequest request;

	@Before
	public void setup() {
		MockitoAnnotations.initMocks(this);
		this.interceptor = new CloudFoundrySecurityInterceptor(this.tokenValidator,
				this.securityService, "my-app-id");
		this.request = new MockHttpServletRequest();
	}

	@Test
	public void preHandleWhenRequestIsPreFlightShouldReturnTrue() {
		this.request.setMethod("OPTIONS");
		this.request.addHeader(HttpHeaders.ORIGIN, "http://example.com");
		this.request.addHeader(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET");
		SecurityResponse response = this.interceptor.preHandle(this.request,
				EndpointId.of("test"));
		assertThat(response.getStatus()).isEqualTo(HttpStatus.OK);
	}

	@Test
	public void preHandleWhenTokenIsMissingShouldReturnFalse() {
		SecurityResponse response = this.interceptor.preHandle(this.request,
				EndpointId.of("test"));
		assertThat(response.getStatus())
				.isEqualTo(Reason.MISSING_AUTHORIZATION.getStatus());
	}

	@Test
	public void preHandleWhenTokenIsNotBearerShouldReturnFalse() {
		this.request.addHeader("Authorization", mockAccessToken());
		SecurityResponse response = this.interceptor.preHandle(this.request,
				EndpointId.of("test"));
		assertThat(response.getStatus())
				.isEqualTo(Reason.MISSING_AUTHORIZATION.getStatus());
	}

	@Test
	public void preHandleWhenApplicationIdIsNullShouldReturnFalse() {
		this.interceptor = new CloudFoundrySecurityInterceptor(this.tokenValidator,
				this.securityService, null);
		this.request.addHeader("Authorization", "bearer " + mockAccessToken());
		SecurityResponse response = this.interceptor.preHandle(this.request,
				EndpointId.of("test"));
		assertThat(response.getStatus())
				.isEqualTo(Reason.SERVICE_UNAVAILABLE.getStatus());
	}

	@Test
	public void preHandleWhenCloudFoundrySecurityServiceIsNullShouldReturnFalse() {
		this.interceptor = new CloudFoundrySecurityInterceptor(this.tokenValidator, null,
				"my-app-id");
		this.request.addHeader("Authorization", "bearer " + mockAccessToken());
		SecurityResponse response = this.interceptor.preHandle(this.request,
				EndpointId.of("test"));
		assertThat(response.getStatus())
				.isEqualTo(Reason.SERVICE_UNAVAILABLE.getStatus());
	}

	@Test
	public void preHandleWhenAccessIsNotAllowedShouldReturnFalse() {
		String accessToken = mockAccessToken();
		this.request.addHeader("Authorization", "bearer " + accessToken);
		given(this.securityService.getAccessLevel(accessToken, "my-app-id"))
				.willReturn(AccessLevel.RESTRICTED);
		SecurityResponse response = this.interceptor.preHandle(this.request,
				EndpointId.of("test"));
		assertThat(response.getStatus()).isEqualTo(Reason.ACCESS_DENIED.getStatus());
	}

	@Test
	public void preHandleSuccessfulWithFullAccess() {
		String accessToken = mockAccessToken();
		this.request.addHeader("Authorization", "Bearer " + accessToken);
		given(this.securityService.getAccessLevel(accessToken, "my-app-id"))
				.willReturn(AccessLevel.FULL);
		SecurityResponse response = this.interceptor.preHandle(this.request,
				EndpointId.of("test"));
		ArgumentCaptor<Token> tokenArgumentCaptor = ArgumentCaptor.forClass(Token.class);
		verify(this.tokenValidator).validate(tokenArgumentCaptor.capture());
		Token token = tokenArgumentCaptor.getValue();
		assertThat(token.toString()).isEqualTo(accessToken);
		assertThat(response.getStatus()).isEqualTo(HttpStatus.OK);
		assertThat(this.request.getAttribute("cloudFoundryAccessLevel"))
				.isEqualTo(AccessLevel.FULL);
	}

	@Test
	public void preHandleSuccessfulWithRestrictedAccess() {
		String accessToken = mockAccessToken();
		this.request.addHeader("Authorization", "Bearer " + accessToken);
		given(this.securityService.getAccessLevel(accessToken, "my-app-id"))
				.willReturn(AccessLevel.RESTRICTED);
		SecurityResponse response = this.interceptor.preHandle(this.request,
				EndpointId.of("info"));
		ArgumentCaptor<Token> tokenArgumentCaptor = ArgumentCaptor.forClass(Token.class);
		verify(this.tokenValidator).validate(tokenArgumentCaptor.capture());
		Token token = tokenArgumentCaptor.getValue();
		assertThat(token.toString()).isEqualTo(accessToken);
		assertThat(response.getStatus()).isEqualTo(HttpStatus.OK);
		assertThat(this.request.getAttribute("cloudFoundryAccessLevel"))
				.isEqualTo(AccessLevel.RESTRICTED);
	}

	private String mockAccessToken() {
		return "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJ0b3B0YWwu"
				+ "Y29tIiwiZXhwIjoxNDI2NDIwODAwLCJhd2Vzb21lIjp0cnVlfQ."
				+ Base64Utils.encodeToString("signature".getBytes());
	}

}
