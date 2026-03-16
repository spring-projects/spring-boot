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

package org.springframework.boot.grpc.server.autoconfigure.security.web.servlet;

import io.grpc.BindableService;
import io.grpc.ServerServiceDefinition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.boot.grpc.server.autoconfigure.security.web.servlet.GrpcRequest.GrpcServletRequestMatcher;
import org.springframework.grpc.server.service.DefaultGrpcServiceDiscoverer;
import org.springframework.grpc.server.service.GrpcServiceDiscoverer;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockServletContext;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.GenericWebApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Test for {@link GrpcRequest}.
 *
 * @author Dave Syer
 * @author Phillip Webb
 */
class GrpcRequestTests {

	private GenericWebApplicationContext context = new GenericWebApplicationContext();

	@BeforeEach
	void setup() {
		MockService service1 = mock();
		given(service1.bindService()).willReturn(ServerServiceDefinition.builder("my-service").build());
		MockService service2 = mock();
		given(service2.bindService()).willReturn(ServerServiceDefinition.builder("my-other-service").build());
		this.context.registerBean("s1", BindableService.class, () -> service1);
		this.context.registerBean("s2", BindableService.class, () -> service2);
		this.context.registerBean(GrpcServiceDiscoverer.class, () -> new DefaultGrpcServiceDiscoverer(this.context));
		this.context.refresh();
	}

	@Test
	void whenToAnyService() {
		GrpcServletRequestMatcher matcher = GrpcRequest.toAnyService();
		assertThat(isMatch(matcher, "/my-service/Method")).isTrue();
		assertThat(isMatch(matcher, "/my-service/Other")).isTrue();
		assertThat(isMatch(matcher, "/my-other-service/Other")).isTrue();
		assertThat(isMatch(matcher, "/notaservice")).isFalse();
	}

	@Test
	void whenToAnyServiceWithExclude() {
		GrpcServletRequestMatcher matcher = GrpcRequest.toAnyService().excluding("my-other-service");
		assertThat(isMatch(matcher, "/my-service/Method")).isTrue();
		assertThat(isMatch(matcher, "/my-service/Other")).isTrue();
		assertThat(isMatch(matcher, "/my-other-service/Other")).isFalse();
		assertThat(isMatch(matcher, "/notaservice")).isFalse();
	}

	private boolean isMatch(GrpcServletRequestMatcher matcher, String path) {
		MockHttpServletRequest request = mockRequest(path);
		return matcher.matches(request);
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
