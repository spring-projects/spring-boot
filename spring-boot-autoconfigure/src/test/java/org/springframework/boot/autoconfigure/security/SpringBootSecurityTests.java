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

package org.springframework.boot.autoconfigure.security;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import org.springframework.boot.autoconfigure.web.servlet.error.ErrorController;
import org.springframework.boot.endpoint.Endpoint;
import org.springframework.boot.endpoint.EndpointPathResolver;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link SpringBootSecurity}.
 *
 * @author Madhura Bhave
 */
public class SpringBootSecurityTests {

	private SpringBootSecurity bootSecurity;

	private EndpointPathResolver endpointPathResolver = new TestEndpointPathResolver();

	private ErrorController errorController = new TestErrorController();

	private MockHttpServletRequest request = new MockHttpServletRequest();

	private static String[] STATIC_RESOURCES = new String[] { "/css/**", "/js/**",
			"/images/**", "/webjars/**", "/**/favicon.ico" };

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	@Before
	public void setUp() throws Exception {
		this.bootSecurity = new SpringBootSecurity(this.endpointPathResolver,
				this.errorController);
	}

	@Test
	public void endpointIdsShouldThrowIfNoEndpointPaths() throws Exception {
		this.thrown.expect(IllegalArgumentException.class);
		this.thrown.expectMessage("At least one endpoint id must be specified.");
		this.bootSecurity.endpointIds();
	}

	@Test
	public void endpointIdsShouldReturnRequestMatcherWithEndpointPaths()
			throws Exception {
		RequestMatcher requestMatcher = this.bootSecurity.endpointIds("id-1", "id-2");
		assertThat(requestMatcher).isInstanceOf(OrRequestMatcher.class);
		this.request.setServletPath("/test/id-1");
		assertThat(requestMatcher.matches(this.request)).isTrue();
		this.request.setServletPath("/test/id-2");
		assertThat(requestMatcher.matches(this.request)).isTrue();
		this.request.setServletPath("/test/other-id");
		assertThat(requestMatcher.matches(this.request)).isFalse();
	}

	@Test
	public void endpointIdsShouldReturnRequestMatcherWithAllEndpointPaths()
			throws Exception {
		RequestMatcher requestMatcher = this.bootSecurity
				.endpointIds(SpringBootSecurity.ALL_ENDPOINTS);
		this.request.setServletPath("/test/id-1");
		assertThat(requestMatcher.matches(this.request)).isTrue();
		this.request.setServletPath("/test/id-2");
		assertThat(requestMatcher.matches(this.request)).isTrue();
		this.request.setServletPath("/test/other-id");
		assertThat(requestMatcher.matches(this.request)).isTrue();
	}

	@Test
	public void endpointsShouldReturnRequestMatcherWithEndpointPaths() throws Exception {
		RequestMatcher requestMatcher = this.bootSecurity.endpoints(TestEndpoint1.class);
		assertThat(requestMatcher).isInstanceOf(OrRequestMatcher.class);
		this.request.setServletPath("/test/id-1");
		assertThat(requestMatcher.matches(this.request)).isTrue();
		this.request.setServletPath("/test/id-2");
		assertThat(requestMatcher.matches(this.request)).isFalse();
	}

	@Test
	public void endpointsShouldThrowIfNoEndpointPaths() throws Exception {
		this.thrown.expect(IllegalArgumentException.class);
		this.thrown.expectMessage("At least one endpoint must be specified.");
		this.bootSecurity.endpoints();
	}

	@Test
	public void endpointsShouldThrowExceptionWhenClassNotEndpoint() throws Exception {
		this.thrown.expect(IllegalArgumentException.class);
		this.thrown.expectMessage("Only classes annotated with @Endpoint are supported.");
		this.bootSecurity.endpoints(FakeEndpoint.class);
	}

	@Test
	public void staticResourcesShouldReturnRequestMatcherWithStaticResources()
			throws Exception {
		RequestMatcher requestMatcher = this.bootSecurity.staticResources();
		assertThat(requestMatcher).isInstanceOf(OrRequestMatcher.class);
		for (String resource : STATIC_RESOURCES) {
			this.request.setServletPath(resource);
			assertThat(requestMatcher.matches(this.request)).isTrue();
		}
	}

	@Test
	public void errorShouldReturnRequestMatcherWithErrorControllerPath()
			throws Exception {
		RequestMatcher requestMatcher = this.bootSecurity.error();
		assertThat(requestMatcher).isInstanceOf(AntPathRequestMatcher.class);
		this.request.setServletPath("/test/error");
		assertThat(requestMatcher.matches(this.request)).isTrue();
	}

	@Test
	public void errorShouldThrowExceptionWhenNoErrorController() throws Exception {
		this.thrown.expect(IllegalStateException.class);
		this.thrown.expectMessage("Path for error controller could not be determined.");
		this.bootSecurity = new SpringBootSecurity(this.endpointPathResolver, null);
		this.bootSecurity.error();
	}

	static class TestEndpointPathResolver implements EndpointPathResolver {

		@Override
		public String resolvePath(String endpointId) {
			return "/test/" + endpointId;
		}

	}

	static class TestErrorController implements ErrorController {

		@Override
		public String getErrorPath() {
			return "/test/error";
		}

	}

	@Endpoint(id = "id-1")
	static class TestEndpoint1 {

	}

	@Endpoint(id = "id-2")
	static class TestEndpoint2 {

	}

	static class FakeEndpoint {

	}

}
