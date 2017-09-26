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

package org.springframework.boot.actuate.autoconfigure.security;

import java.util.Arrays;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.assertj.core.api.AssertDelegateTarget;
import org.junit.Test;

import org.springframework.boot.actuate.autoconfigure.endpoint.web.EndpointPathProvider;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockServletContext;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.StaticWebApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link EndpointRequest}.
 *
 * @author Phillip Webb
 */
public class EndpointRequestTests {

	@Test
	public void toAnyEndpointShouldMatchEndpointPath() throws Exception {
		RequestMatcher matcher = EndpointRequest.toAnyEndpoint();
		assertMatcher(matcher).matches("/application/foo");
		assertMatcher(matcher).matches("/application/bar");
	}

	@Test
	public void toAnyEndpointShouldNotMatchOtherPath() throws Exception {
		RequestMatcher matcher = EndpointRequest.toAnyEndpoint();
		assertMatcher(matcher).doesNotMatch("/application/baz");
	}

	@Test
	public void toEndpointClassShouldMatchEndpointPath() throws Exception {
		RequestMatcher matcher = EndpointRequest.to(FooEndpoint.class);
		assertMatcher(matcher).matches("/application/foo");
	}

	@Test
	public void toEndpointClassShouldNotMatchOtherPath() throws Exception {
		RequestMatcher matcher = EndpointRequest.to(FooEndpoint.class);
		assertMatcher(matcher).doesNotMatch("/application/bar");
	}

	@Test
	public void toEndpointIdShouldMatchEndpointPath() throws Exception {
		RequestMatcher matcher = EndpointRequest.to("foo");
		assertMatcher(matcher).matches("/application/foo");
	}

	@Test
	public void toEndpointIdShouldNotMatchOtherPath() throws Exception {
		RequestMatcher matcher = EndpointRequest.to("foo");
		assertMatcher(matcher).doesNotMatch("/application/bar");
	}

	@Test
	public void excludeByClassShouldNotMatchExcluded() throws Exception {
		RequestMatcher matcher = EndpointRequest.toAnyEndpoint()
				.excluding(FooEndpoint.class);
		assertMatcher(matcher).doesNotMatch("/application/foo");
		assertMatcher(matcher).matches("/application/bar");
	}

	@Test
	public void excludeByIdShouldNotMatchExcluded() throws Exception {
		RequestMatcher matcher = EndpointRequest.toAnyEndpoint().excluding("foo");
		assertMatcher(matcher).doesNotMatch("/application/foo");
		assertMatcher(matcher).matches("/application/bar");
	}

	private RequestMatcherAssert assertMatcher(RequestMatcher matcher) {
		return assertMatcher(matcher, new MockEndpointPathProvider());
	}

	private RequestMatcherAssert assertMatcher(RequestMatcher matcher,
			EndpointPathProvider endpointPathProvider) {
		StaticWebApplicationContext context = new StaticWebApplicationContext();
		context.registerBean(EndpointPathProvider.class, () -> endpointPathProvider);
		return assertThat(new RequestMatcherAssert(context, matcher));
	}

	private static class RequestMatcherAssert implements AssertDelegateTarget {

		private final WebApplicationContext context;

		private final RequestMatcher matcher;

		RequestMatcherAssert(WebApplicationContext context, RequestMatcher matcher) {
			this.context = context;
			this.matcher = matcher;
		}

		public void matches(String path) {
			matches(mockRequest(path));
		}

		private void matches(HttpServletRequest request) {
			assertThat(this.matcher.matches(request))
					.as("Matches " + getRequestPath(request)).isTrue();
		}

		public void doesNotMatch(String path) {
			doesNotMatch(mockRequest(path));
		}

		private void doesNotMatch(HttpServletRequest request) {
			assertThat(this.matcher.matches(request))
					.as("Does not match " + getRequestPath(request)).isFalse();
		}

		private MockHttpServletRequest mockRequest(String path) {
			return mockRequest(null, path);
		}

		private MockHttpServletRequest mockRequest(String servletPath, String path) {
			MockServletContext servletContext = new MockServletContext();
			servletContext.setAttribute(
					WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE,
					this.context);
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

	private static class MockEndpointPathProvider implements EndpointPathProvider {

		@Override
		public List<String> getPaths() {
			return Arrays.asList("/application/foo", "/application/bar");
		}

		@Override
		public String getPath(String id) {
			if ("foo".equals(id)) {
				return "/application/foo";
			}
			if ("bar".equals(id)) {
				return "/application/bar";
			}
			return null;
		}

	}

	@Endpoint(id = "foo")
	private static class FooEndpoint {

	}
}
