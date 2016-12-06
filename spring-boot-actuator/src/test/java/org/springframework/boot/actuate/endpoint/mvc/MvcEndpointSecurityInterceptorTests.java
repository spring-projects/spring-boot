/*
 * Copyright 2012-2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.actuate.endpoint.mvc;

import java.util.Arrays;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import org.springframework.boot.actuate.endpoint.AbstractEndpoint;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockServletContext;
import org.springframework.web.method.HandlerMethod;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link MvcEndpointSecurityInterceptor}.
 *
 * @author Madhura Bhave
 */
public class MvcEndpointSecurityInterceptorTests {

	private MvcEndpointSecurityInterceptor securityInterceptor;

	private TestMvcEndpoint mvcEndpoint;

	private TestEndpoint endpoint;

	private HandlerMethod handlerMethod;

	private MockHttpServletRequest request;

	private MockHttpServletResponse response;

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
		this.response = new MockHttpServletResponse();
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
	public void sensitiveEndpointIfRoleIsNotPresentShouldNotAllowAccess()
			throws Exception {
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
