/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.grpc.server.autoconfigure.security.web.reactive;

import java.util.List;

import io.grpc.BindableService;
import io.grpc.ServerServiceDefinition;
import org.junit.jupiter.api.Test;

import org.springframework.boot.grpc.server.autoconfigure.security.web.reactive.GrpcRequest.GrpcReactiveRequestMatcher;
import org.springframework.boot.test.context.assertj.AssertableReactiveWebApplicationContext;
import org.springframework.boot.test.context.runner.ContextConsumer;
import org.springframework.boot.test.context.runner.ReactiveWebApplicationContextRunner;
import org.springframework.boot.web.context.reactive.GenericReactiveWebApplicationContext;
import org.springframework.boot.web.context.reactive.ReactiveWebApplicationContext;
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

	@Test
	void whenToAnyService() {
		run(List.of("my-service", "my-other-service"), (context) -> {
			GrpcReactiveRequestMatcher matcher = GrpcRequest.toAnyService();
			assertThat(isMatch(matcher, context, "/my-service/Method")).isTrue();
			assertThat(isMatch(matcher, context, "/my-service/Other")).isTrue();
			assertThat(isMatch(matcher, context, "/my-other-service/Other")).isTrue();
			assertThat(isMatch(matcher, context, "/notaservice")).isFalse();
		});
	}

	@Test
	void whenToAnyServiceWithExclude() {
		run(List.of("my-service", "my-other-service"), (context) -> {
			GrpcReactiveRequestMatcher matcher = GrpcRequest.toAnyService().excluding("my-other-service");
			assertThat(isMatch(matcher, context, "/my-service/Method")).isTrue();
			assertThat(isMatch(matcher, context, "/my-service/Other")).isTrue();
			assertThat(isMatch(matcher, context, "/my-other-service/Other")).isFalse();
			assertThat(isMatch(matcher, context, "/notaservice")).isFalse();
		});
	}

	private boolean isMatch(GrpcReactiveRequestMatcher matcher, ReactiveWebApplicationContext context, String path) {
		MockExchange request = mockRequest(context, path);
		MatchResult result = matcher.matches(request).block();
		return (result != null) && result.isMatch();
	}

	private MockExchange mockRequest(ReactiveWebApplicationContext context, String path) {
		MockServerHttpRequest servletContext = MockServerHttpRequest.get(path).build();
		MockExchange request = new MockExchange(servletContext, context);
		return request;
	}

	private void run(List<String> services, ContextConsumer<AssertableReactiveWebApplicationContext> consumer) {
		ReactiveWebApplicationContextRunner runner = new ReactiveWebApplicationContextRunner(
				GenericReactiveWebApplicationContext::new);
		for (int i = 0; i < services.size(); i++) {
			MockService mockService = mockService(services.get(i));
			runner = runner.withBean("s%d".formatted(i + 1), BindableService.class, () -> mockService);
		}
		runner
			.withInitializer((context) -> ((GenericReactiveWebApplicationContext) context)
				.registerBean(GrpcServiceDiscoverer.class, () -> new DefaultGrpcServiceDiscoverer(context)))
			.run(consumer);
	}

	private MockService mockService(String name) {
		MockService service = mock();
		given(service.bindService()).willReturn(ServerServiceDefinition.builder(name).build());
		return service;
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
