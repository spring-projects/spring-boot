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

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.BDDMockito;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import org.springframework.boot.actuate.cloudfoundry.CloudFoundryAuthorizationException.Reason;
import org.springframework.boot.actuate.endpoint.AbstractEndpoint;
import org.springframework.boot.actuate.endpoint.mvc.EndpointMvcAdapter;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.util.Base64Utils;
import org.springframework.web.method.HandlerMethod;

import static org.assertj.core.api.Assertions.assertThat;
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

	private TestMvcEndpoint endpoint;

	private HandlerMethod handlerMethod;

	private MockHttpServletRequest request;

	private MockHttpServletResponse response;

	@Before
	public void setup() throws Exception {
		MockitoAnnotations.initMocks(this);
		this.interceptor = new CloudFoundrySecurityInterceptor(this.tokenValidator,
				this.securityService, "my-app-id");
		this.endpoint = new TestMvcEndpoint(new TestEndpoint("a"));
		this.handlerMethod = new HandlerMethod(this.endpoint, "invoke");
		this.request = new MockHttpServletRequest();
		this.response = new MockHttpServletResponse();
	}

	@Test
	public void preHandleWhenRequestIsPreFlightShouldReturnTrue() throws Exception {
		this.request.setMethod("OPTIONS");
		this.request.addHeader(HttpHeaders.ORIGIN, "http://example.com");
		this.request.addHeader(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET");
		boolean preHandle = this.interceptor.preHandle(this.request, this.response,
				this.handlerMethod);
		assertThat(preHandle).isTrue();
	}

	@Test
	public void preHandleWhenTokenIsMissingShouldReturnFalse() throws Exception {
		boolean preHandle = this.interceptor.preHandle(this.request, this.response,
				this.handlerMethod);
		assertThat(preHandle).isFalse();
		assertThat(this.response.getStatus())
				.isEqualTo(Reason.MISSING_AUTHORIZATION.getStatus().value());
		assertThat(this.response.getContentAsString()).contains("security_error");
		assertThat(this.response.getContentType())
				.isEqualTo(MediaType.APPLICATION_JSON.toString());
	}

	@Test
	public void preHandleWhenTokenIsNotBearerShouldReturnFalse() throws Exception {
		this.request.addHeader("Authorization", mockAccessToken());
		boolean preHandle = this.interceptor.preHandle(this.request, this.response,
				this.handlerMethod);
		assertThat(preHandle).isFalse();
		assertThat(this.response.getStatus())
				.isEqualTo(Reason.MISSING_AUTHORIZATION.getStatus().value());
	}

	@Test
	public void preHandleWhenApplicationIdIsNullShouldReturnFalse() throws Exception {
		this.interceptor = new CloudFoundrySecurityInterceptor(this.tokenValidator,
				this.securityService, null);
		this.request.addHeader("Authorization", "bearer " + mockAccessToken());
		boolean preHandle = this.interceptor.preHandle(this.request, this.response,
				this.handlerMethod);
		assertThat(preHandle).isFalse();
		assertThat(this.response.getStatus())
				.isEqualTo(Reason.SERVICE_UNAVAILABLE.getStatus().value());
	}

	@Test
	public void preHandleWhenCloudFoundrySecurityServiceIsNullShouldReturnFalse()
			throws Exception {
		this.interceptor = new CloudFoundrySecurityInterceptor(this.tokenValidator, null,
				"my-app-id");
		this.request.addHeader("Authorization", "bearer " + mockAccessToken());
		boolean preHandle = this.interceptor.preHandle(this.request, this.response,
				this.handlerMethod);
		assertThat(preHandle).isFalse();
		assertThat(this.response.getStatus())
				.isEqualTo(Reason.SERVICE_UNAVAILABLE.getStatus().value());
	}

	@Test
	public void preHandleWhenAccessIsNotAllowedShouldReturnFalse() throws Exception {
		this.endpoint = new TestMvcEndpoint(new TestEndpoint("env"));
		this.handlerMethod = new HandlerMethod(this.endpoint, "invoke");
		String accessToken = mockAccessToken();
		this.request.addHeader("Authorization", "bearer " + accessToken);
		BDDMockito.given(this.securityService.getAccessLevel(accessToken, "my-app-id"))
				.willReturn(AccessLevel.RESTRICTED);
		boolean preHandle = this.interceptor.preHandle(this.request, this.response,
				this.handlerMethod);
		assertThat(preHandle).isFalse();
		assertThat(this.response.getStatus())
				.isEqualTo(Reason.ACCESS_DENIED.getStatus().value());
	}

	@Test
	public void preHandleSuccessfulWithFullAccess() throws Exception {
		String accessToken = mockAccessToken();
		this.request.addHeader("Authorization", "Bearer " + accessToken);
		BDDMockito.given(this.securityService.getAccessLevel(accessToken, "my-app-id"))
				.willReturn(AccessLevel.FULL);
		boolean preHandle = this.interceptor.preHandle(this.request, this.response,
				this.handlerMethod);
		ArgumentCaptor<Token> tokenArgumentCaptor = ArgumentCaptor.forClass(Token.class);
		verify(this.tokenValidator).validate(tokenArgumentCaptor.capture());
		Token token = tokenArgumentCaptor.getValue();
		assertThat(token.toString()).isEqualTo(accessToken);
		assertThat(preHandle).isTrue();
		assertThat(this.response.getStatus()).isEqualTo(HttpStatus.OK.value());
		assertThat(this.request.getAttribute("cloudFoundryAccessLevel"))
				.isEqualTo(AccessLevel.FULL);
	}

	@Test
	public void preHandleSuccessfulWithRestrictedAccess() throws Exception {
		this.endpoint = new TestMvcEndpoint(new TestEndpoint("info"));
		this.handlerMethod = new HandlerMethod(this.endpoint, "invoke");
		String accessToken = mockAccessToken();
		this.request.addHeader("Authorization", "Bearer " + accessToken);
		BDDMockito.given(this.securityService.getAccessLevel(accessToken, "my-app-id"))
				.willReturn(AccessLevel.RESTRICTED);
		boolean preHandle = this.interceptor.preHandle(this.request, this.response,
				this.handlerMethod);
		ArgumentCaptor<Token> tokenArgumentCaptor = ArgumentCaptor.forClass(Token.class);
		verify(this.tokenValidator).validate(tokenArgumentCaptor.capture());
		Token token = tokenArgumentCaptor.getValue();
		assertThat(token.toString()).isEqualTo(accessToken);
		assertThat(preHandle).isTrue();
		assertThat(this.response.getStatus()).isEqualTo(HttpStatus.OK.value());
		assertThat(this.request.getAttribute("cloudFoundryAccessLevel"))
				.isEqualTo(AccessLevel.RESTRICTED);
	}

	private String mockAccessToken() {
		return "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJ0b3B0YWwu"
				+ "Y29tIiwiZXhwIjoxNDI2NDIwODAwLCJhd2Vzb21lIjp0cnVlfQ."
				+ Base64Utils.encodeToString("signature".getBytes());
	}

	private static class TestEndpoint extends AbstractEndpoint<Object> {

		TestEndpoint(String id) {
			super(id);
		}

		@Override
		public Object invoke() {
			return null;
		}

	}

	private static class TestMvcEndpoint extends EndpointMvcAdapter {

		TestMvcEndpoint(TestEndpoint delegate) {
			super(delegate);
		}

	}

}
