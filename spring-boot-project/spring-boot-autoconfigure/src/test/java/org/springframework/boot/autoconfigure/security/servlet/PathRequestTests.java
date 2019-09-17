/*
 * Copyright 2012-2019 the original author or authors.
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

package org.springframework.boot.autoconfigure.security.servlet;

import javax.servlet.http.HttpServletRequest;

import org.assertj.core.api.AssertDelegateTarget;
import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.h2.H2ConsoleProperties;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockServletContext;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.context.WebApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link PathRequest}.
 *
 * @author Madhura Bhave
 */
class PathRequestTests {

	@Test
	void toStaticResourcesShouldReturnStaticResourceRequest() {
		assertThat(PathRequest.toStaticResources()).isInstanceOf(StaticResourceRequest.class);
	}

	@Test
	void toH2ConsoleShouldMatchH2ConsolePath() {
		RequestMatcher matcher = PathRequest.toH2Console();
		assertMatcher(matcher).matches("/h2-console");
		assertMatcher(matcher).matches("/h2-console/subpath");
		assertMatcher(matcher).doesNotMatch("/js/file.js");
	}

	@Test
	void toH2ConsoleWhenManagementContextShouldNeverMatch() {
		RequestMatcher matcher = PathRequest.toH2Console();
		assertMatcher(matcher, "management").doesNotMatch("/h2-console");
		assertMatcher(matcher, "management").doesNotMatch("/h2-console/subpath");
		assertMatcher(matcher, "management").doesNotMatch("/js/file.js");
	}

	private RequestMatcherAssert assertMatcher(RequestMatcher matcher) {
		return assertMatcher(matcher, null);
	}

	private RequestMatcherAssert assertMatcher(RequestMatcher matcher, String serverNamespace) {
		TestWebApplicationContext context = new TestWebApplicationContext(serverNamespace);
		context.registerBean(ServerProperties.class);
		context.registerBean(H2ConsoleProperties.class);
		return assertThat(new RequestMatcherAssert(context, matcher));
	}

	static class RequestMatcherAssert implements AssertDelegateTarget {

		private final WebApplicationContext context;

		private final RequestMatcher matcher;

		RequestMatcherAssert(WebApplicationContext context, RequestMatcher matcher) {
			this.context = context;
			this.matcher = matcher;
		}

		void matches(String path) {
			matches(mockRequest(path));
		}

		private void matches(HttpServletRequest request) {
			assertThat(this.matcher.matches(request)).as("Matches " + getRequestPath(request)).isTrue();
		}

		void doesNotMatch(String path) {
			doesNotMatch(mockRequest(path));
		}

		private void doesNotMatch(HttpServletRequest request) {
			assertThat(this.matcher.matches(request)).as("Does not match " + getRequestPath(request)).isFalse();
		}

		private MockHttpServletRequest mockRequest(String path) {
			MockServletContext servletContext = new MockServletContext();
			servletContext.setAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE, this.context);
			MockHttpServletRequest request = new MockHttpServletRequest(servletContext);
			request.setPathInfo(path);
			return request;
		}

		private String getRequestPath(HttpServletRequest request) {
			String url = request.getServletPath();
			if (request.getPathInfo() != null) {
				url += request.getPathInfo();
			}
			return url;
		}

	}

}
