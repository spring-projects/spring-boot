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

package org.springframework.boot.grpc.server.autoconfigure.security;

import io.grpc.BindableService;
import io.grpc.ServerServiceDefinition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.boot.grpc.server.autoconfigure.security.GrpcServletRequest.GrpcServletRequestMatcher;
import org.springframework.grpc.server.service.DefaultGrpcServiceDiscoverer;
import org.springframework.grpc.server.service.GrpcServiceDiscoverer;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockServletContext;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.StaticWebApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

class GrpcServletRequestTests {

	private StaticWebApplicationContext context = new StaticWebApplicationContext();

	@BeforeEach
	void setup() {
		MockService service = mock();
		ServerServiceDefinition serviceDefinition = ServerServiceDefinition.builder("my-service").build();
		given(service.bindService()).willReturn(serviceDefinition);
		this.context.registerBean(BindableService.class, () -> service);
		this.context.registerBean(GrpcServiceDiscoverer.class, () -> new DefaultGrpcServiceDiscoverer(this.context));
	}

	@Test
	void requestMatches() {
		GrpcServletRequestMatcher matcher = GrpcServletRequest.all();
		MockHttpServletRequest request = mockRequest("/my-service/Method");
		assertThat(matcher.matches(request)).isTrue();
	}

	@Test
	void noMatch() {
		GrpcServletRequestMatcher matcher = GrpcServletRequest.all();
		MockHttpServletRequest request = mockRequest("/other-service/Method");
		assertThat(matcher.matches(request)).isFalse();
	}

	@Test
	void requestMatcherExcludes() {
		GrpcServletRequestMatcher matcher = GrpcServletRequest.all().excluding("my-service");
		MockHttpServletRequest request = mockRequest("/my-service/Method");
		assertThat(matcher.matches(request)).isFalse();
	}

	@Test
	void noServices() {
		GrpcServletRequestMatcher matcher = GrpcServletRequest.all();
		MockHttpServletRequest request = mockRequestNoServices("/my-service/Method");
		assertThat(matcher.matches(request)).isFalse();
	}

	private MockHttpServletRequest mockRequestNoServices(String path) {
		MockServletContext servletContext = new MockServletContext();
		servletContext.setAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE,
				new StaticWebApplicationContext());
		MockHttpServletRequest request = new MockHttpServletRequest(servletContext);
		request.setPathInfo(path);
		return request;
	}

	private MockHttpServletRequest mockRequest(String path) {
		MockServletContext servletContext = new MockServletContext();
		servletContext.setAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE, this.context);
		MockHttpServletRequest request = new MockHttpServletRequest(servletContext);
		request.setRequestURI(path);
		return request;
	}

	interface MockService extends BindableService {

	}

}
