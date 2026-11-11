/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.grpc.server.autoconfigure.security.web.servlet;

import java.util.List;

import io.grpc.BindableService;
import io.grpc.ServerServiceDefinition;
import org.junit.jupiter.api.Test;

import org.springframework.boot.grpc.server.autoconfigure.security.web.servlet.GrpcRequest.GrpcServletRequestMatcher;
import org.springframework.boot.test.context.assertj.AssertableWebApplicationContext;
import org.springframework.boot.test.context.runner.ContextConsumer;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
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

	@Test
	void whenToAnyService() {
		run(List.of("my-service", "my-other-service"), (context) -> {
			GrpcServletRequestMatcher matcher = GrpcRequest.toAnyService();
			assertThat(isMatch(matcher, context, "/my-service/Method")).isTrue();
			assertThat(isMatch(matcher, context, "/my-service/Other")).isTrue();
			assertThat(isMatch(matcher, context, "/my-other-service/Other")).isTrue();
			assertThat(isMatch(matcher, context, "/notaservice")).isFalse();
		});
	}

	@Test
	void whenToAnyServiceWithExclude() {
		run(List.of("my-service", "my-other-service"), (context) -> {
			GrpcServletRequestMatcher matcher = GrpcRequest.toAnyService().excluding("my-other-service");
			assertThat(isMatch(matcher, context, "/my-service/Method")).isTrue();
			assertThat(isMatch(matcher, context, "/my-service/Other")).isTrue();
			assertThat(isMatch(matcher, context, "/my-other-service/Other")).isFalse();
			assertThat(isMatch(matcher, context, "/notaservice")).isFalse();
		});
	}

	private boolean isMatch(GrpcServletRequestMatcher matcher, WebApplicationContext context, String path) {
		MockHttpServletRequest request = mockRequest(context, path);
		return matcher.matches(request);
	}

	private MockHttpServletRequest mockRequest(WebApplicationContext context, String path) {
		MockServletContext servletContext = new MockServletContext();
		servletContext.setAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE, context);
		MockHttpServletRequest request = new MockHttpServletRequest(servletContext);
		request.setRequestURI(path);
		return request;
	}

	private void run(List<String> services, ContextConsumer<AssertableWebApplicationContext> consumer) {
		WebApplicationContextRunner runner = new WebApplicationContextRunner(GenericWebApplicationContext::new);
		for (int i = 0; i < services.size(); i++) {
			MockService mockService = mockService(services.get(i));
			runner = runner.withBean("s%d".formatted(i + 1), BindableService.class, () -> mockService);
		}
		runner
			.withInitializer((context) -> ((GenericWebApplicationContext) context)
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

}
