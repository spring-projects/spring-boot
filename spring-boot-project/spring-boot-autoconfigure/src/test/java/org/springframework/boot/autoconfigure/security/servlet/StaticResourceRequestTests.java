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

import org.springframework.boot.autoconfigure.security.StaticResourceLocation;
import org.springframework.boot.autoconfigure.web.servlet.DispatcherServletPath;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockServletContext;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.context.WebApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Tests for {@link StaticResourceRequest}.
 *
 * @author Madhura Bhave
 * @author Phillip Webb
 */
class StaticResourceRequestTests {

	private StaticResourceRequest resourceRequest = StaticResourceRequest.INSTANCE;

	@Test
	void atCommonLocationsShouldMatchCommonLocations() {
		RequestMatcher matcher = this.resourceRequest.atCommonLocations();
		assertMatcher(matcher).matches("/css/file.css");
		assertMatcher(matcher).matches("/js/file.js");
		assertMatcher(matcher).matches("/images/file.css");
		assertMatcher(matcher).matches("/webjars/file.css");
		assertMatcher(matcher).matches("/foo/favicon.ico");
		assertMatcher(matcher).doesNotMatch("/bar");
	}

	@Test
	void atCommonLocationsWhenManagementContextShouldNeverMatch() {
		RequestMatcher matcher = this.resourceRequest.atCommonLocations();
		assertMatcher(matcher, "management").doesNotMatch("/css/file.css");
		assertMatcher(matcher, "management").doesNotMatch("/js/file.js");
		assertMatcher(matcher, "management").doesNotMatch("/images/file.css");
		assertMatcher(matcher, "management").doesNotMatch("/webjars/file.css");
		assertMatcher(matcher, "management").doesNotMatch("/foo/favicon.ico");
	}

	@Test
	void atCommonLocationsWithExcludeShouldNotMatchExcluded() {
		RequestMatcher matcher = this.resourceRequest.atCommonLocations().excluding(StaticResourceLocation.CSS);
		assertMatcher(matcher).doesNotMatch("/css/file.css");
		assertMatcher(matcher).matches("/js/file.js");
	}

	@Test
	void atLocationShouldMatchLocation() {
		RequestMatcher matcher = this.resourceRequest.at(StaticResourceLocation.CSS);
		assertMatcher(matcher).matches("/css/file.css");
		assertMatcher(matcher).doesNotMatch("/js/file.js");
	}

	@Test
	void atLocationWhenHasServletPathShouldMatchLocation() {
		RequestMatcher matcher = this.resourceRequest.at(StaticResourceLocation.CSS);
		assertMatcher(matcher, null, "/foo").matches("/foo", "/css/file.css");
		assertMatcher(matcher, null, "/foo").doesNotMatch("/foo", "/js/file.js");
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

	private RequestMatcherAssert assertMatcher(RequestMatcher matcher) {
		return assertMatcher(matcher, null, "");
	}

	private RequestMatcherAssert assertMatcher(RequestMatcher matcher, String serverNamespace) {
		return assertMatcher(matcher, serverNamespace, "");
	}

	private RequestMatcherAssert assertMatcher(RequestMatcher matcher, String serverNamespace, String path) {
		DispatcherServletPath dispatcherServletPath = () -> path;
		TestWebApplicationContext context = new TestWebApplicationContext(serverNamespace);
		context.registerBean(DispatcherServletPath.class, () -> dispatcherServletPath);
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

		void matches(String servletPath, String path) {
			matches(mockRequest(servletPath, path));
		}

		private void matches(HttpServletRequest request) {
			assertThat(this.matcher.matches(request)).as("Matches " + getRequestPath(request)).isTrue();
		}

		void doesNotMatch(String path) {
			doesNotMatch(mockRequest(path));
		}

		void doesNotMatch(String servletPath, String path) {
			doesNotMatch(mockRequest(servletPath, path));
		}

		private void doesNotMatch(HttpServletRequest request) {
			assertThat(this.matcher.matches(request)).as("Does not match " + getRequestPath(request)).isFalse();
		}

		private MockHttpServletRequest mockRequest(String path) {
			return mockRequest(null, path);
		}

		private MockHttpServletRequest mockRequest(String servletPath, String path) {
			MockServletContext servletContext = new MockServletContext();
			servletContext.setAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE, this.context);
			MockHttpServletRequest request = new MockHttpServletRequest(servletContext);
			if (servletPath != null) {
				request.setServletPath(servletPath);
			}
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
