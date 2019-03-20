/*
 * Copyright 2012-2017 the original author or authors.
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

package org.springframework.boot.actuate.endpoint.mvc;

import java.security.Principal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import javax.servlet.http.HttpServletResponse;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import org.springframework.boot.actuate.endpoint.AbstractEndpoint;
import org.springframework.boot.test.rule.OutputCapture;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockServletContext;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.method.HandlerMethod;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Tests for {@link MvcEndpointSecurityInterceptor}.
 *
 * @author Madhura Bhave
 */
public class MvcEndpointSecurityInterceptorTests {

	@Rule
	public OutputCapture output = new OutputCapture();

	private MvcEndpointSecurityInterceptor securityInterceptor;

	private TestMvcEndpoint mvcEndpoint;

	private TestEndpoint endpoint;

	private HandlerMethod handlerMethod;

	private MockHttpServletRequest request;

	private HttpServletResponse response;

	private MockServletContext servletContext;

	private List<String> roles;

	@Before
	public void setup() throws Exception {
		this.roles = Arrays.asList("SUPER_HERO");
		this.securityInterceptor = new MvcEndpointSecurityInterceptor(true, this.roles);
		this.endpoint = new TestEndpoint("a");
		this.mvcEndpoint = new TestMvcEndpoint(this.endpoint);
		this.handlerMethod = new HandlerMethod(this.mvcEndpoint, "invoke");
		this.servletContext = new MockServletContext();
		this.request = new MockHttpServletRequest(this.servletContext);
		this.response = mock(HttpServletResponse.class);
	}

	@Test
	public void securityDisabledShouldAllowAccess() throws Exception {
		this.securityInterceptor = new MvcEndpointSecurityInterceptor(false, this.roles);
		assertThat(this.securityInterceptor.preHandle(this.request, this.response,
				this.handlerMethod)).isTrue();
	}

	@Test
	public void endpointNotSensitiveShouldAllowAccess() throws Exception {
		this.endpoint.setSensitive(false);
		assertThat(this.securityInterceptor.preHandle(this.request, this.response,
				this.handlerMethod)).isTrue();
	}

	@Test
	public void sensitiveEndpointIfRoleIsPresentShouldAllowAccess() throws Exception {
		this.servletContext.declareRoles("SUPER_HERO");
		assertThat(this.securityInterceptor.preHandle(this.request, this.response,
				this.handlerMethod)).isTrue();
	}

	@Test
	public void sensitiveEndpointIfNotAuthenticatedShouldNotAllowAccess()
			throws Exception {
		assertThat(this.securityInterceptor.preHandle(this.request, this.response,
				this.handlerMethod)).isFalse();
		verify(this.response).sendError(HttpStatus.UNAUTHORIZED.value(),
				"Full authentication is required to access this resource.");
		assertThat(this.securityInterceptor.preHandle(this.request, this.response,
				this.handlerMethod)).isFalse();
		assertThat(this.output.toString())
				.containsOnlyOnce("Full authentication is required to access actuator "
						+ "endpoints. Consider adding Spring Security or set "
						+ "'management.security.enabled' to false");
	}

	@Test
	public void sensitiveEndpointIfRoleIsNotCorrectShouldNotAllowAccess()
			throws Exception {
		Principal principal = mock(Principal.class);
		this.request.setUserPrincipal(principal);
		this.servletContext.declareRoles("HERO");
		assertThat(this.securityInterceptor.preHandle(this.request, this.response,
				this.handlerMethod)).isFalse();
		verify(this.response).sendError(HttpStatus.FORBIDDEN.value(),
				"Access is denied. User must have one of the these roles: SUPER_HERO");
	}

	@Test
	public void sensitiveEndpointIfRoleNotCorrectShouldCheckAuthorities()
			throws Exception {
		Principal principal = mock(Principal.class);
		this.request.setUserPrincipal(principal);
		Authentication authentication = mock(Authentication.class);
		Set<SimpleGrantedAuthority> authorities = Collections
				.singleton(new SimpleGrantedAuthority("SUPER_HERO"));
		doReturn(authorities).when(authentication).getAuthorities();
		SecurityContextHolder.getContext().setAuthentication(authentication);
		assertThat(this.securityInterceptor.preHandle(this.request, this.response,
				this.handlerMethod)).isTrue();
	}

	@Test
	public void sensitiveEndpointIfRoleAndAuthoritiesNotCorrectShouldNotAllowAccess()
			throws Exception {
		Principal principal = mock(Principal.class);
		this.request.setUserPrincipal(principal);
		Authentication authentication = mock(Authentication.class);
		Set<SimpleGrantedAuthority> authorities = Collections
				.singleton(new SimpleGrantedAuthority("HERO"));
		doReturn(authorities).when(authentication).getAuthorities();
		SecurityContextHolder.getContext().setAuthentication(authentication);
		assertThat(this.securityInterceptor.preHandle(this.request, this.response,
				this.handlerMethod)).isFalse();
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
