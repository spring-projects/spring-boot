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

import org.springframework.boot.grpc.server.autoconfigure.security.GrpcReactiveRequest.GrpcReactiveRequestMatcher;
import org.springframework.context.ApplicationContext;
import org.springframework.grpc.server.service.DefaultGrpcServiceDiscoverer;
import org.springframework.grpc.server.service.GrpcServiceDiscoverer;
import org.springframework.http.codec.ServerCodecConfigurer;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.http.server.reactive.MockServerHttpResponse;
import org.springframework.web.context.support.StaticWebApplicationContext;
import org.springframework.web.server.adapter.DefaultServerWebExchange;
import org.springframework.web.server.i18n.AcceptHeaderLocaleContextResolver;
import org.springframework.web.server.session.DefaultWebSessionManager;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

class GrpcReactiveRequestTests {

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
		GrpcReactiveRequestMatcher matcher = GrpcReactiveRequest.all();
		MockExchange request = mockRequest("/my-service/Method");
		assertThat(matcher.matches(request).block().isMatch()).isTrue();
	}

	private MockExchange mockRequest(String path) {
		MockServerHttpRequest servletContext = MockServerHttpRequest.get(path).build();
		MockExchange request = new MockExchange(servletContext, this.context);
		return request;
	}

	interface MockService extends BindableService {

	}

	static class MockExchange extends DefaultServerWebExchange {

		private ApplicationContext context;

		MockExchange(MockServerHttpRequest request, ApplicationContext context) {
			super(request, new MockServerHttpResponse(), new DefaultWebSessionManager(), ServerCodecConfigurer.create(),
					new AcceptHeaderLocaleContextResolver());
			this.context = context;
		}

		@Override
		public ApplicationContext getApplicationContext() {
			return this.context;
		}

	}

}
