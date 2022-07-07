/*
 * Copyright 2012-2022 the original author or authors.
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

package org.springframework.boot.actuate.autoconfigure.security.reactive;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import org.assertj.core.api.AssertDelegateTarget;
import org.junit.jupiter.api.Test;

import org.springframework.boot.actuate.autoconfigure.endpoint.web.WebEndpointProperties;
import org.springframework.boot.actuate.endpoint.EndpointId;
import org.springframework.boot.actuate.endpoint.ExposableEndpoint;
import org.springframework.boot.actuate.endpoint.Operation;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.web.PathMappedEndpoint;
import org.springframework.boot.actuate.endpoint.web.PathMappedEndpoints;
import org.springframework.boot.actuate.endpoint.web.annotation.ServletEndpoint;
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
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link EndpointRequest}.
 *
 * @author Madhura Bhave
 * @author Phillip Webb
 */
class EndpointRequestTests {

	@Test
	void toAnyEndpointShouldMatchEndpointPath() {
		ServerWebExchangeMatcher matcher = EndpointRequest.toAnyEndpoint();
		assertMatcher(matcher).matches("/actuator/foo");
		assertMatcher(matcher).matches("/actuator/bar");
		assertMatcher(matcher).matches("/actuator");
	}

	@Test
	void toAnyEndpointShouldMatchEndpointPathWithTrailingSlash() {
		ServerWebExchangeMatcher matcher = EndpointRequest.toAnyEndpoint();
		assertMatcher(matcher).matches("/actuator/foo/");
		assertMatcher(matcher).matches("/actuator/bar/");
		assertMatcher(matcher).matches("/actuator/");
	}

	@Test
	void toAnyEndpointWhenBasePathIsEmptyShouldNotMatchLinks() {
		ServerWebExchangeMatcher matcher = EndpointRequest.toAnyEndpoint();
		RequestMatcherAssert assertMatcher = assertMatcher(matcher, "");
		assertMatcher.doesNotMatch("/");
		assertMatcher.matches("/foo");
		assertMatcher.matches("/bar");
	}

	@Test
	void toAnyEndpointShouldNotMatchOtherPath() {
		ServerWebExchangeMatcher matcher = EndpointRequest.toAnyEndpoint();
		assertMatcher(matcher).doesNotMatch("/actuator/baz");
	}

	@Test
	void toEndpointClassShouldMatchEndpointPath() {
		ServerWebExchangeMatcher matcher = EndpointRequest.to(FooEndpoint.class);
		assertMatcher(matcher).matches("/actuator/foo");
		assertMatcher(matcher).matches("/actuator/foo/");
	}

	@Test
	void toEndpointClassShouldNotMatchOtherPath() {
		ServerWebExchangeMatcher matcher = EndpointRequest.to(FooEndpoint.class);
		assertMatcher(matcher).doesNotMatch("/actuator/bar");
		assertMatcher(matcher).doesNotMatch("/actuator/bar/");
	}

	@Test
	void toEndpointIdShouldMatchEndpointPath() {
		ServerWebExchangeMatcher matcher = EndpointRequest.to("foo");
		assertMatcher(matcher).matches("/actuator/foo");
		assertMatcher(matcher).matches("/actuator/foo/");
	}

	@Test
	void toEndpointIdShouldNotMatchOtherPath() {
		ServerWebExchangeMatcher matcher = EndpointRequest.to("foo");
		assertMatcher(matcher).doesNotMatch("/actuator/bar");
		assertMatcher(matcher).doesNotMatch("/actuator/bar/");
	}

	@Test
	void toLinksShouldOnlyMatchLinks() {
		ServerWebExchangeMatcher matcher = EndpointRequest.toLinks();
		assertMatcher(matcher).doesNotMatch("/actuator/foo");
		assertMatcher(matcher).doesNotMatch("/actuator/bar");
		assertMatcher(matcher).matches("/actuator");
		assertMatcher(matcher).matches("/actuator/");
	}

	@Test
	void toLinksWhenBasePathEmptyShouldNotMatch() {
		ServerWebExchangeMatcher matcher = EndpointRequest.toLinks();
		RequestMatcherAssert assertMatcher = assertMatcher(matcher, "");
		assertMatcher.doesNotMatch("/actuator/foo");
		assertMatcher.doesNotMatch("/actuator/bar");
		assertMatcher.doesNotMatch("/");
	}

	@Test
	void excludeByClassShouldNotMatchExcluded() {
		ServerWebExchangeMatcher matcher = EndpointRequest.toAnyEndpoint().excluding(FooEndpoint.class,
				BazServletEndpoint.class);
		List<ExposableEndpoint<?>> endpoints = new ArrayList<>();
		endpoints.add(mockEndpoint(EndpointId.of("foo"), "foo"));
		endpoints.add(mockEndpoint(EndpointId.of("bar"), "bar"));
		endpoints.add(mockEndpoint(EndpointId.of("baz"), "baz"));
		PathMappedEndpoints pathMappedEndpoints = new PathMappedEndpoints("/actuator", () -> endpoints);
		assertMatcher(matcher, pathMappedEndpoints).doesNotMatch("/actuator/foo");
		assertMatcher(matcher, pathMappedEndpoints).doesNotMatch("/actuator/foo/");
		assertMatcher(matcher, pathMappedEndpoints).doesNotMatch("/actuator/baz");
		assertMatcher(matcher, pathMappedEndpoints).doesNotMatch("/actuator/baz/");
		assertMatcher(matcher).matches("/actuator/bar");
		assertMatcher(matcher).matches("/actuator/bar/");
		assertMatcher(matcher).matches("/actuator");
		assertMatcher(matcher).matches("/actuator/");
	}

	@Test
	void excludeByClassShouldNotMatchLinksIfExcluded() {
		ServerWebExchangeMatcher matcher = EndpointRequest.toAnyEndpoint().excludingLinks()
				.excluding(FooEndpoint.class);
		assertMatcher(matcher).doesNotMatch("/actuator/foo");
		assertMatcher(matcher).doesNotMatch("/actuator/foo/");
		assertMatcher(matcher).doesNotMatch("/actuator");
		assertMatcher(matcher).doesNotMatch("/actuator/");
	}

	@Test
	void excludeByIdShouldNotMatchExcluded() {
		ServerWebExchangeMatcher matcher = EndpointRequest.toAnyEndpoint().excluding("foo");
		assertMatcher(matcher).doesNotMatch("/actuator/foo");
		assertMatcher(matcher).doesNotMatch("/actuator/foo/");
		assertMatcher(matcher).matches("/actuator/bar");
		assertMatcher(matcher).matches("/actuator/bar/");
		assertMatcher(matcher).matches("/actuator");
		assertMatcher(matcher).matches("/actuator/");
	}

	@Test
	void excludeByIdShouldNotMatchLinksIfExcluded() {
		ServerWebExchangeMatcher matcher = EndpointRequest.toAnyEndpoint().excludingLinks().excluding("foo");
		assertMatcher(matcher).doesNotMatch("/actuator/foo");
		assertMatcher(matcher).doesNotMatch("/actuator/foo/");
		assertMatcher(matcher).doesNotMatch("/actuator");
		assertMatcher(matcher).doesNotMatch("/actuator/");
	}

	@Test
	void excludeLinksShouldNotMatchBasePath() {
		ServerWebExchangeMatcher matcher = EndpointRequest.toAnyEndpoint().excludingLinks();
		assertMatcher(matcher).doesNotMatch("/actuator");
		assertMatcher(matcher).doesNotMatch("/actuator/");
		assertMatcher(matcher).matches("/actuator/foo");
		assertMatcher(matcher).matches("/actuator/foo/");
		assertMatcher(matcher).matches("/actuator/bar");
		assertMatcher(matcher).matches("/actuator/bar/");
	}

	@Test
	void excludeLinksShouldNotMatchBasePathIfEmptyAndExcluded() {
		ServerWebExchangeMatcher matcher = EndpointRequest.toAnyEndpoint().excludingLinks();
		RequestMatcherAssert assertMatcher = assertMatcher(matcher, "");
		assertMatcher.doesNotMatch("/");
		assertMatcher.matches("/foo");
		assertMatcher.matches("/foo/");
		assertMatcher.matches("/bar");
		assertMatcher.matches("/bar/");
	}

	@Test
	void noEndpointPathsBeansShouldNeverMatch() {
		ServerWebExchangeMatcher matcher = EndpointRequest.toAnyEndpoint();
		assertMatcher(matcher, (PathMappedEndpoints) null).doesNotMatch("/actuator/foo");
		assertMatcher(matcher, (PathMappedEndpoints) null).doesNotMatch("/actuator/foo/");
		assertMatcher(matcher, (PathMappedEndpoints) null).doesNotMatch("/actuator/bar");
		assertMatcher(matcher, (PathMappedEndpoints) null).doesNotMatch("/actuator/bar/");
	}

	private RequestMatcherAssert assertMatcher(ServerWebExchangeMatcher matcher) {
		return assertMatcher(matcher, mockPathMappedEndpoints("/actuator"));
	}

	private RequestMatcherAssert assertMatcher(ServerWebExchangeMatcher matcher, String basePath) {
		return assertMatcher(matcher, mockPathMappedEndpoints(basePath));
	}

	private PathMappedEndpoints mockPathMappedEndpoints(String basePath) {
		List<ExposableEndpoint<?>> endpoints = new ArrayList<>();
		endpoints.add(mockEndpoint(EndpointId.of("foo"), "foo"));
		endpoints.add(mockEndpoint(EndpointId.of("bar"), "bar"));
		return new PathMappedEndpoints(basePath, () -> endpoints);
	}

	private TestEndpoint mockEndpoint(EndpointId id, String rootPath) {
		TestEndpoint endpoint = mock(TestEndpoint.class);
		given(endpoint.getEndpointId()).willReturn(id);
		given(endpoint.getRootPath()).willReturn(rootPath);
		return endpoint;
	}

	private RequestMatcherAssert assertMatcher(ServerWebExchangeMatcher matcher,
			PathMappedEndpoints pathMappedEndpoints) {
		StaticApplicationContext context = new StaticApplicationContext();
		context.registerBean(WebEndpointProperties.class);
		if (pathMappedEndpoints != null) {
			context.registerBean(PathMappedEndpoints.class, () -> pathMappedEndpoints);
			WebEndpointProperties properties = context.getBean(WebEndpointProperties.class);
			if (!properties.getBasePath().equals(pathMappedEndpoints.getBasePath())) {
				properties.setBasePath(pathMappedEndpoints.getBasePath());
			}
		}
		return assertThat(new RequestMatcherAssert(context, matcher));
	}

	static class RequestMatcherAssert implements AssertDelegateTarget {

		private final StaticApplicationContext context;

		private final ServerWebExchangeMatcher matcher;

		RequestMatcherAssert(StaticApplicationContext context, ServerWebExchangeMatcher matcher) {
			this.context = context;
			this.matcher = matcher;
		}

		void matches(String path) {
			ServerWebExchange exchange = webHandler().createExchange(MockServerHttpRequest.get(path).build(),
					new MockServerHttpResponse());
			matches(exchange);
		}

		private void matches(ServerWebExchange exchange) {
			assertThat(this.matcher.matches(exchange).block(Duration.ofSeconds(30)).isMatch())
					.as("Matches " + getRequestPath(exchange)).isTrue();
		}

		void doesNotMatch(String path) {
			ServerWebExchange exchange = webHandler().createExchange(MockServerHttpRequest.get(path).build(),
					new MockServerHttpResponse());
			doesNotMatch(exchange);
		}

		private void doesNotMatch(ServerWebExchange exchange) {
			assertThat(this.matcher.matches(exchange).block(Duration.ofSeconds(30)).isMatch())
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

	static class TestHttpWebHandlerAdapter extends HttpWebHandlerAdapter {

		TestHttpWebHandlerAdapter(WebHandler delegate) {
			super(delegate);
		}

		@Override
		protected ServerWebExchange createExchange(ServerHttpRequest request, ServerHttpResponse response) {
			return super.createExchange(request, response);
		}

	}

	@Endpoint(id = "foo")
	static class FooEndpoint {

	}

	@ServletEndpoint(id = "baz")
	static class BazServletEndpoint {

	}

	interface TestEndpoint extends ExposableEndpoint<Operation>, PathMappedEndpoint {

	}

}
