/*
 * Copyright 2012-2017 the original author or authors.
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

package org.springframework.boot.actuate.autoconfigure.security.reactive;

import java.util.Arrays;
import java.util.List;

import org.assertj.core.api.AssertDelegateTarget;
import org.junit.Test;

import org.springframework.boot.actuate.autoconfigure.endpoint.web.EndpointPathProvider;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.context.support.StaticApplicationContext;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.http.server.reactive.MockServerHttpResponse;
import org.springframework.security.web.server.util.matcher.ServerWebExchangeMatcher;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebHandler;
import org.springframework.web.server.adapter.HttpWebHandlerAdapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link EndpointRequest}.
 *
 * @author Madhura Bhave
 */
public class EndpointRequestTests {

	@Test
	public void toAnyEndpointShouldMatchEndpointPath() {
		ServerWebExchangeMatcher matcher = EndpointRequest.toAnyEndpoint();
		assertMatcher(matcher).matches("/actuator/foo");
		assertMatcher(matcher).matches("/actuator/bar");
	}

	@Test
	public void toAnyEndpointShouldNotMatchOtherPath() {
		ServerWebExchangeMatcher matcher = EndpointRequest.toAnyEndpoint();
		assertMatcher(matcher).doesNotMatch("/actuator/baz");
	}

	@Test
	public void toEndpointClassShouldMatchEndpointPath() {
		ServerWebExchangeMatcher matcher = EndpointRequest.to(FooEndpoint.class);
		assertMatcher(matcher).matches("/actuator/foo");
	}

	@Test
	public void toEndpointClassShouldNotMatchOtherPath() {
		ServerWebExchangeMatcher matcher = EndpointRequest.to(FooEndpoint.class);
		assertMatcher(matcher).doesNotMatch("/actuator/bar");
	}

	@Test
	public void toEndpointIdShouldMatchEndpointPath() {
		ServerWebExchangeMatcher matcher = EndpointRequest.to("foo");
		assertMatcher(matcher).matches("/actuator/foo");
	}

	@Test
	public void toEndpointIdShouldNotMatchOtherPath() {
		ServerWebExchangeMatcher matcher = EndpointRequest.to("foo");
		assertMatcher(matcher).doesNotMatch("/actuator/bar");
	}

	@Test
	public void excludeByClassShouldNotMatchExcluded() {
		ServerWebExchangeMatcher matcher = EndpointRequest.toAnyEndpoint()
				.excluding(FooEndpoint.class);
		assertMatcher(matcher).doesNotMatch("/actuator/foo");
		assertMatcher(matcher).matches("/actuator/bar");
	}

	@Test
	public void excludeByIdShouldNotMatchExcluded() {
		ServerWebExchangeMatcher matcher = EndpointRequest.toAnyEndpoint().excluding("foo");
		assertMatcher(matcher).doesNotMatch("/actuator/foo");
		assertMatcher(matcher).matches("/actuator/bar");
	}

	private RequestMatcherAssert assertMatcher(ServerWebExchangeMatcher matcher) {
		return assertMatcher(matcher, new MockEndpointPathProvider());
	}

	private RequestMatcherAssert assertMatcher(ServerWebExchangeMatcher matcher,
			EndpointPathProvider endpointPathProvider) {
		StaticApplicationContext context = new StaticApplicationContext();
		context.registerBean(EndpointPathProvider.class, () -> endpointPathProvider);
		return assertThat(new RequestMatcherAssert(context, matcher));
	}

	private static class RequestMatcherAssert implements AssertDelegateTarget {

		private final StaticApplicationContext context;

		private final ServerWebExchangeMatcher matcher;

		RequestMatcherAssert(StaticApplicationContext context, ServerWebExchangeMatcher matcher) {
			this.context = context;
			this.matcher = matcher;
		}

		void matches(String path) {
			ServerWebExchange exchange = webHandler().createExchange(MockServerHttpRequest.get(path).build(), new MockServerHttpResponse());
			matches(exchange);
		}

		private void matches(ServerWebExchange exchange) {
			assertThat(this.matcher.matches(exchange).block().isMatch())
					.as("Matches " + getRequestPath(exchange)).isTrue();
		}

		void doesNotMatch(String path) {
			ServerWebExchange exchange = webHandler().createExchange(MockServerHttpRequest.get(path).build(), new MockServerHttpResponse());
			doesNotMatch(exchange);
		}

		private void doesNotMatch(ServerWebExchange exchange) {
			assertThat(this.matcher.matches(exchange).block().isMatch())
					.as("Does not match " + getRequestPath(exchange)).isFalse();
		}

		private TestHttpWebHandlerAdapter webHandler() {
			TestHttpWebHandlerAdapter adapter = new TestHttpWebHandlerAdapter(mock(WebHandler.class));
			adapter.setApplicationContext(this.context);
			return adapter;
		}

		private String getRequestPath(ServerWebExchange exchange) {
			return exchange.getRequest().getPath().toString();
		}

	}

	private static class TestHttpWebHandlerAdapter extends HttpWebHandlerAdapter {

		TestHttpWebHandlerAdapter(WebHandler delegate) {
			super(delegate);
		}

		@Override
		protected ServerWebExchange createExchange(ServerHttpRequest request, ServerHttpResponse response) {
			return super.createExchange(request, response);
		}

	}

	private static class MockEndpointPathProvider implements EndpointPathProvider {

		@Override
		public List<String> getPaths() {
			return Arrays.asList("/actuator/foo", "/actuator/bar");
		}

		@Override
		public String getPath(String id) {
			if ("foo".equals(id)) {
				return "/actuator/foo";
			}
			if ("bar".equals(id)) {
				return "/actuator/bar";
			}
			return null;
		}

	}

	@Endpoint(id = "foo")
	private static class FooEndpoint {

	}
}
