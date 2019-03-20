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
import java.util.List;

import javax.servlet.http.HttpServletResponse;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.boot.actuate.endpoint.AbstractEndpoint;
import org.springframework.boot.junit.runner.classpath.ClassPathExclusions;
import org.springframework.boot.junit.runner.classpath.ModifiedClassPathRunner;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockServletContext;
import org.springframework.web.method.HandlerMethod;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link MvcEndpointSecurityInterceptor} when Spring Security is not available.
 *
 * @author Madhura Bhave
 */
@RunWith(ModifiedClassPathRunner.class)
@ClassPathExclusions("spring-security-*.jar")
public class NoSpringSecurityMvcEndpointSecurityInterceptorTests {

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
	public void sensitiveEndpointIfRoleNotPresentShouldNotValidateAuthorities()
			throws Exception {
		Principal principal = mock(Principal.class);
		this.request.setUserPrincipal(principal);
		this.servletContext.declareRoles("HERO");
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
