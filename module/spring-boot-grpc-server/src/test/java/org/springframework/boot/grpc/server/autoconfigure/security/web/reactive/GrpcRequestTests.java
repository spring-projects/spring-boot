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

package org.springframework.boot.grpc.server.autoconfigure.security.web.reactive;

import io.grpc.BindableService;
import io.grpc.ServerServiceDefinition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.boot.grpc.server.autoconfigure.security.web.reactive.GrpcRequest.GrpcReactiveRequestMatcher;
import org.springframework.boot.web.context.reactive.GenericReactiveWebApplicationContext;
import org.springframework.context.ApplicationContext;
import org.springframework.grpc.server.service.DefaultGrpcServiceDiscoverer;
import org.springframework.grpc.server.service.GrpcServiceDiscoverer;
import org.springframework.http.codec.ServerCodecConfigurer;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.http.server.reactive.MockServerHttpResponse;
import org.springframework.security.web.server.util.matcher.ServerWebExchangeMatcher.MatchResult;
import org.springframework.web.server.adapter.DefaultServerWebExchange;
import org.springframework.web.server.i18n.AcceptHeaderLocaleContextResolver;
import org.springframework.web.server.session.DefaultWebSessionManager;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link GrpcRequest}.
 *
 * @author Dave Syer
 * @author Phillip Webb
 */
class GrpcRequestTests {

	private GenericReactiveWebApplicationContext context = new GenericReactiveWebApplicationContext();

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
		GrpcReactiveRequestMatcher matcher = GrpcRequest.toAnyService();
		assertThat(isMatch(matcher, "/my-service/Method")).isTrue();
		assertThat(isMatch(matcher, "/my-service/Other")).isTrue();
		assertThat(isMatch(matcher, "/my-other-service/Other")).isTrue();
		assertThat(isMatch(matcher, "/notaservice")).isFalse();
	}

	@Test
	void whenToAnyServiceWithExclude() {
		GrpcReactiveRequestMatcher matcher = GrpcRequest.toAnyService().excluding("my-other-service");
		assertThat(isMatch(matcher, "/my-service/Method")).isTrue();
		assertThat(isMatch(matcher, "/my-service/Other")).isTrue();
		assertThat(isMatch(matcher, "/my-other-service/Other")).isFalse();
		assertThat(isMatch(matcher, "/notaservice")).isFalse();
	}

	private boolean isMatch(GrpcReactiveRequestMatcher matcher, String path) {
		MockExchange request = mockRequest(path);
		MatchResult result = matcher.matches(request).block();
		return (result != null) && result.isMatch();
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
