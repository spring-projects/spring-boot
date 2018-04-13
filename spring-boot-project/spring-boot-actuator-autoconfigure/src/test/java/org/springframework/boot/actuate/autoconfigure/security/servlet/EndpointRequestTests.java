/*
 * Copyright 2012-2018 the original author or authors.
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

package org.springframework.boot.actuate.autoconfigure.security.servlet;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.assertj.core.api.AssertDelegateTarget;
import org.junit.Test;

import org.springframework.boot.actuate.autoconfigure.endpoint.web.WebEndpointProperties;
import org.springframework.boot.actuate.endpoint.ExposableEndpoint;
import org.springframework.boot.actuate.endpoint.Operation;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.web.PathMappedEndpoint;
import org.springframework.boot.actuate.endpoint.web.PathMappedEndpoints;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockServletContext;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.StaticWebApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link EndpointRequest}.
 *
 * @author Phillip Webb
 * @author Madhura Bhave
 */
public class EndpointRequestTests {

	@Test
	public void toAnyEndpointShouldMatchEndpointPath() {
		RequestMatcher matcher = EndpointRequest.toAnyEndpoint();
		assertMatcher(matcher).matches("/actuator/foo");
		assertMatcher(matcher).matches("/actuator/bar");
		assertMatcher(matcher).matches("/actuator");
	}

	@Test
	public void toAnyEndpointWhenBasePathIsEmptyShouldNotMatchLinks() {
		RequestMatcher matcher = EndpointRequest.toAnyEndpoint();
		RequestMatcherAssert assertMatcher = assertMatcher(matcher, "");
		assertMatcher.doesNotMatch("/");
		assertMatcher.matches("/foo");
		assertMatcher.matches("/bar");
	}

	@Test
	public void toAnyEndpointShouldNotMatchOtherPath() {
		RequestMatcher matcher = EndpointRequest.toAnyEndpoint();
		assertMatcher(matcher).doesNotMatch("/actuator/baz");
	}

	@Test
	public void toEndpointClassShouldMatchEndpointPath() {
		RequestMatcher matcher = EndpointRequest.to(FooEndpoint.class);
		assertMatcher(matcher).matches("/actuator/foo");
	}

	@Test
	public void toEndpointClassShouldNotMatchOtherPath() {
		RequestMatcher matcher = EndpointRequest.to(FooEndpoint.class);
		assertMatcher(matcher).doesNotMatch("/actuator/bar");
		assertMatcher(matcher).doesNotMatch("/actuator");
	}

	@Test
	public void toEndpointIdShouldMatchEndpointPath() {
		RequestMatcher matcher = EndpointRequest.to("foo");
		assertMatcher(matcher).matches("/actuator/foo");
	}

	@Test
	public void toEndpointIdShouldNotMatchOtherPath() {
		RequestMatcher matcher = EndpointRequest.to("foo");
		assertMatcher(matcher).doesNotMatch("/actuator/bar");
		assertMatcher(matcher).doesNotMatch("/actuator");
	}

	@Test
	public void toLinksShouldOnlyMatchLinks() {
		RequestMatcher matcher = EndpointRequest.toLinks();
		assertMatcher(matcher).doesNotMatch("/actuator/foo");
		assertMatcher(matcher).doesNotMatch("/actuator/bar");
		assertMatcher(matcher).matches("/actuator");
	}

	@Test
	public void toLinksWhenBasePathEmptyShouldNotMatch() {
		RequestMatcher matcher = EndpointRequest.toLinks();
		RequestMatcherAssert assertMatcher = assertMatcher(matcher, "");
		assertMatcher.doesNotMatch("/actuator/foo");
		assertMatcher.doesNotMatch("/actuator/bar");
		assertMatcher.doesNotMatch("/");
	}

	@Test
	public void excludeByClassShouldNotMatchExcluded() {
		RequestMatcher matcher = EndpointRequest.toAnyEndpoint()
				.excluding(FooEndpoint.class);
		assertMatcher(matcher).doesNotMatch("/actuator/foo");
		assertMatcher(matcher).matches("/actuator/bar");
		assertMatcher(matcher).matches("/actuator");
	}

	@Test
	public void excludeByClassShouldNotMatchLinksIfExcluded() {
		RequestMatcher matcher = EndpointRequest.toAnyEndpoint().excludingLinks()
				.excluding(FooEndpoint.class);
		assertMatcher(matcher).doesNotMatch("/actuator/foo");
		assertMatcher(matcher).doesNotMatch("/actuator");
	}

	@Test
	public void excludeByIdShouldNotMatchExcluded() {
		RequestMatcher matcher = EndpointRequest.toAnyEndpoint().excluding("foo");
		assertMatcher(matcher).doesNotMatch("/actuator/foo");
		assertMatcher(matcher).matches("/actuator/bar");
		assertMatcher(matcher).matches("/actuator");
	}

	@Test
	public void excludeByIdShouldNotMatchLinksIfExcluded() {
		RequestMatcher matcher = EndpointRequest.toAnyEndpoint().excludingLinks()
				.excluding("foo");
		assertMatcher(matcher).doesNotMatch("/actuator/foo");
		assertMatcher(matcher).doesNotMatch("/actuator");
	}

	@Test
	public void excludeLinksShouldNotMatchBasePath() {
		RequestMatcher matcher = EndpointRequest.toAnyEndpoint().excludingLinks();
		assertMatcher(matcher).doesNotMatch("/actuator");
		assertMatcher(matcher).matches("/actuator/foo");
		assertMatcher(matcher).matches("/actuator/bar");
	}

	@Test
	public void excludeLinksShouldNotMatchBasePathIfEmptyAndExcluded() {
		RequestMatcher matcher = EndpointRequest.toAnyEndpoint().excludingLinks();
		RequestMatcherAssert assertMatcher = assertMatcher(matcher, "");
		assertMatcher.doesNotMatch("/");
		assertMatcher.matches("/foo");
		assertMatcher.matches("/bar");
	}

	@Test
	public void noEndpointPathsBeansShouldNeverMatch() {
		RequestMatcher matcher = EndpointRequest.toAnyEndpoint();
		assertMatcher(matcher, (PathMappedEndpoints) null).doesNotMatch("/actuator/foo");
		assertMatcher(matcher, (PathMappedEndpoints) null).doesNotMatch("/actuator/bar");
	}

	private RequestMatcherAssert assertMatcher(RequestMatcher matcher) {
		return assertMatcher(matcher, mockPathMappedEndpoints("/actuator"));
	}

	private RequestMatcherAssert assertMatcher(RequestMatcher matcher, String basePath) {
		return assertMatcher(matcher, mockPathMappedEndpoints(basePath));
	}

	private PathMappedEndpoints mockPathMappedEndpoints(String basePath) {
		List<ExposableEndpoint<?>> endpoints = new ArrayList<>();
		endpoints.add(mockEndpoint("foo", "foo"));
		endpoints.add(mockEndpoint("bar", "bar"));
		return new PathMappedEndpoints(basePath, () -> endpoints);
	}

	private TestEndpoint mockEndpoint(String id, String rootPath) {
		TestEndpoint endpoint = mock(TestEndpoint.class);
		given(endpoint.getId()).willReturn(id);
		given(endpoint.getRootPath()).willReturn(rootPath);
		return endpoint;
	}

	private RequestMatcherAssert assertMatcher(RequestMatcher matcher,
			PathMappedEndpoints pathMappedEndpoints) {
		StaticWebApplicationContext context = new StaticWebApplicationContext();
		context.registerBean(WebEndpointProperties.class);
		if (pathMappedEndpoints != null) {
			context.registerBean(PathMappedEndpoints.class, () -> pathMappedEndpoints);
			WebEndpointProperties properties = context
					.getBean(WebEndpointProperties.class);
			if (!properties.getBasePath().equals(pathMappedEndpoints.getBasePath())) {
				properties.setBasePath(pathMappedEndpoints.getBasePath());
			}
		}
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

	@Endpoint(id = "foo")
	private static class FooEndpoint {

	}

	interface TestEndpoint extends ExposableEndpoint<Operation>, PathMappedEndpoint {

	}

}
