/*
 * Copyright 2012-2023 the original author or authors.
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

package org.springframework.boot.autoconfigure.security.reactive;

import java.time.Duration;

import org.assertj.core.api.AssertDelegateTarget;
import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.security.StaticResourceLocation;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.context.support.StaticApplicationContext;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.http.server.reactive.MockServerHttpResponse;
import org.springframework.security.web.server.util.matcher.ServerWebExchangeMatcher;
import org.springframework.web.context.support.StaticWebApplicationContext;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebHandler;
import org.springframework.web.server.adapter.HttpWebHandlerAdapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link StaticResourceRequest}.
 *
 * @author Madhura Bhave
 */
class StaticResourceRequestTests {

	private final StaticResourceRequest resourceRequest = StaticResourceRequest.INSTANCE;

	@Test
	void atCommonLocationsShouldMatchCommonLocations() {
		ServerWebExchangeMatcher matcher = this.resourceRequest.atCommonLocations();
		assertMatcher(matcher).matches("/css/file.css");
		assertMatcher(matcher).matches("/js/file.js");
		assertMatcher(matcher).matches("/images/file.css");
		assertMatcher(matcher).matches("/webjars/file.css");
		assertMatcher(matcher).matches("/favicon.ico");
		assertMatcher(matcher).matches("/favicon.png");
		assertMatcher(matcher).matches("/icons/icon-48x48.png");
		assertMatcher(matcher).doesNotMatch("/bar");
	}

	@Test
	void atCommonLocationsWithExcludeShouldNotMatchExcluded() {
		ServerWebExchangeMatcher matcher = this.resourceRequest.atCommonLocations()
			.excluding(StaticResourceLocation.CSS);
		assertMatcher(matcher).doesNotMatch("/css/file.css");
		assertMatcher(matcher).matches("/js/file.js");
	}

	@Test
	void atLocationShouldMatchLocation() {
		ServerWebExchangeMatcher matcher = this.resourceRequest.at(StaticResourceLocation.CSS);
		assertMatcher(matcher).matches("/css/file.css");
		assertMatcher(matcher).doesNotMatch("/js/file.js");
	}

	@Test
	void atLocationsFromSetWhenSetIsNullShouldThrowException() {
		assertThatIllegalArgumentException().isThrownBy(() -> this.resourceRequest.at(null))
			.withMessageContaining("Locations must not be null");
	}

	@Test
	void excludeFromSetWhenSetIsNullShouldThrowException() {
		assertThatIllegalArgumentException().isThrownBy(() -> this.resourceRequest.atCommonLocations().excluding(null))
			.withMessageContaining("Locations must not be null");
	}

	private RequestMatcherAssert assertMatcher(ServerWebExchangeMatcher matcher) {
		StaticWebApplicationContext context = new StaticWebApplicationContext();
		context.registerBean(ServerProperties.class);
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
				.as("Matches " + getRequestPath(exchange))
				.isTrue();
		}

		void doesNotMatch(String path) {
			ServerWebExchange exchange = webHandler().createExchange(MockServerHttpRequest.get(path).build(),
					new MockServerHttpResponse());
			doesNotMatch(exchange);
		}

		private void doesNotMatch(ServerWebExchange exchange) {
			assertThat(this.matcher.matches(exchange).block(Duration.ofSeconds(30)).isMatch())
				.as("Does not match " + getRequestPath(exchange))
				.isFalse();
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

}
