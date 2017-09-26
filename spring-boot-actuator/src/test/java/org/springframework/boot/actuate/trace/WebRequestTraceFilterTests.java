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

package org.springframework.boot.actuate.trace;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.security.Principal;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Map;

import javax.servlet.ServletException;

import org.junit.Test;

import org.springframework.boot.web.servlet.error.DefaultErrorAttributes;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Tests for {@link WebRequestTraceFilter}.
 *
 * @author Dave Syer
 * @author Wallace Wadge
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @author Venil Noronha
 * @author Stephane Nicoll
 * @author Madhura Bhave
 */
public class WebRequestTraceFilterTests {

	private final InMemoryTraceRepository repository = new InMemoryTraceRepository();

	@Test
	@SuppressWarnings("unchecked")
	public void filterAddsTraceWithDefaultIncludes() {
		WebRequestTraceFilter filter = new WebRequestTraceFilter(this.repository);
		MockHttpServletRequest request = spy(new MockHttpServletRequest("GET", "/foo"));
		request.addHeader("Accept", "application/json");
		Map<String, Object> trace = filter.getTrace(request);
		assertThat(trace.get("method")).isEqualTo("GET");
		assertThat(trace.get("path")).isEqualTo("/foo");
		Map<String, Object> map = (Map<String, Object>) trace.get("headers");
		assertThat(map.get("request").toString()).isEqualTo("{Accept=application/json}");
		verify(request, times(0)).getParameterMap();
	}

	@Test
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public void filterAddsTraceWithCustomIncludes() throws IOException, ServletException {
		WebRequestTraceFilter filter = new WebRequestTraceFilter(this.repository,
				EnumSet.allOf(Include.class));
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/foo");
		request.addHeader("Accept", "application/json");
		request.addHeader("Cookie", "testCookie=testValue;");
		request.setContextPath("some.context.path");
		request.setContent("Hello, World!".getBytes());
		request.setRemoteAddr("some.remote.addr");
		request.setQueryString("some.query.string");
		request.setParameter("param", "paramvalue");
		File tmp = File.createTempFile("spring-boot", "tmp");
		String url = tmp.toURI().toURL().toString();
		request.setPathInfo(url);
		tmp.deleteOnExit();
		request.setAuthType("authType");
		Principal principal = () -> "principalTest";
		request.setUserPrincipal(principal);
		MockHttpServletResponse response = new MockHttpServletResponse();
		response.addHeader("Content-Type", "application/json");
		response.addHeader("Set-Cookie", "a=b");
		filter.doFilterInternal(request, response, (req, resp) -> {
			BufferedReader bufferedReader = req.getReader();
			while (bufferedReader.readLine() != null) {
				// read the contents as normal (forces cache to fill up)
			}
			resp.getWriter().println("Goodbye, World!");
		});
		assertThat(this.repository.findAll()).hasSize(1);
		Map<String, Object> trace = this.repository.findAll().iterator().next().getInfo();
		Map<String, Object> map = (Map<String, Object>) trace.get("headers");

		assertThat(map.get("response").toString())
				.isEqualTo("{Content-Type=application/json, Set-Cookie=a=b, status=200}");
		assertThat(trace.get("method")).isEqualTo("GET");
		assertThat(trace.get("path")).isEqualTo("/foo");
		assertThat(((String[]) ((Map) trace.get("parameters")).get("param"))[0])
				.isEqualTo("paramvalue");
		assertThat(trace.get("remoteAddress")).isEqualTo("some.remote.addr");
		assertThat(trace.get("query")).isEqualTo("some.query.string");
		assertThat(trace.get("userPrincipal")).isEqualTo(principal.getName());
		assertThat(trace.get("contextPath")).isEqualTo("some.context.path");
		assertThat(trace.get("pathInfo")).isEqualTo(url);
		assertThat(trace.get("authType")).isEqualTo("authType");
		assertThat(map.get("request").toString())
				.isEqualTo("{Accept=application/json, Cookie=testCookie=testValue;}");
	}

	@Test
	@SuppressWarnings({ "unchecked" })
	public void filterDoesNotAddResponseHeadersWithoutResponseHeadersInclude()
			throws ServletException, IOException {
		WebRequestTraceFilter filter = new WebRequestTraceFilter(this.repository,
				Collections.singleton(Include.REQUEST_HEADERS));
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/foo");
		MockHttpServletResponse response = new MockHttpServletResponse();
		response.addHeader("Content-Type", "application/json");
		filter.doFilterInternal(request, response, (req, resp) -> {
		});
		Map<String, Object> info = this.repository.findAll().iterator().next().getInfo();
		Map<String, Object> headers = (Map<String, Object>) info.get("headers");
		assertThat(headers.get("response") == null).isTrue();
	}

	@Test
	@SuppressWarnings({ "unchecked" })
	public void filterDoesNotAddRequestCookiesWithCookiesExclude()
			throws ServletException, IOException {
		WebRequestTraceFilter filter = new WebRequestTraceFilter(this.repository,
				Collections.singleton(Include.REQUEST_HEADERS));
		MockHttpServletRequest request = spy(new MockHttpServletRequest("GET", "/foo"));
		request.addHeader("Accept", "application/json");
		request.addHeader("Cookie", "testCookie=testValue;");
		Map<String, Object> map = (Map<String, Object>) filter.getTrace(request)
				.get("headers");
		assertThat(map.get("request").toString()).isEqualTo("{Accept=application/json}");
	}

	@Test
	@SuppressWarnings({ "unchecked" })
	public void filterDoesNotAddAuthorizationHeaderWithoutAuthorizationHeaderInclude()
			throws ServletException, IOException {
		WebRequestTraceFilter filter = new WebRequestTraceFilter(this.repository);
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/foo");
		request.addHeader("Authorization", "my-auth-header");
		MockHttpServletResponse response = new MockHttpServletResponse();
		filter.doFilterInternal(request, response, (req, resp) -> {
		});
		Map<String, Object> info = this.repository.findAll().iterator().next().getInfo();
		Map<String, Object> headers = (Map<String, Object>) info.get("headers");
		assertThat(((Map<Object, Object>) headers.get("request"))).hasSize(0);
	}

	@Test
	@SuppressWarnings({ "unchecked" })
	public void filterAddsAuthorizationHeaderWhenAuthorizationHeaderIncluded()
			throws ServletException, IOException {
		WebRequestTraceFilter filter = new WebRequestTraceFilter(this.repository,
				EnumSet.of(Include.REQUEST_HEADERS, Include.AUTHORIZATION_HEADER));
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/foo");
		request.addHeader("Authorization", "my-auth-header");
		MockHttpServletResponse response = new MockHttpServletResponse();
		filter.doFilterInternal(request, response, (req, resp) -> {
		});
		Map<String, Object> info = this.repository.findAll().iterator().next().getInfo();
		Map<String, Object> headers = (Map<String, Object>) info.get("headers");
		assertThat(((Map<Object, Object>) headers.get("request")))
				.containsKey("Authorization");
	}

	@Test
	@SuppressWarnings({ "unchecked" })
	public void filterDoesNotAddResponseCookiesWithCookiesExclude()
			throws ServletException, IOException {
		WebRequestTraceFilter filter = new WebRequestTraceFilter(this.repository,
				Collections.singleton(Include.RESPONSE_HEADERS));
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/foo");
		MockHttpServletResponse response = new MockHttpServletResponse();
		response.addHeader("Content-Type", "application/json");
		response.addHeader("Set-Cookie", "testCookie=testValue;");
		Map<String, Object> trace = filter.getTrace(request);
		filter.enhanceTrace(trace, response);
		Map<String, Object> map = (Map<String, Object>) trace.get("headers");
		assertThat(map.get("response").toString())
				.isEqualTo("{Content-Type=application/json, status=200}");
	}

	@Test
	public void filterHasResponseStatus() {
		WebRequestTraceFilter filter = new WebRequestTraceFilter(this.repository);
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/foo");
		MockHttpServletResponse response = new MockHttpServletResponse();
		response.setStatus(404);
		response.addHeader("Content-Type", "application/json");
		Map<String, Object> trace = filter.getTrace(request);
		filter.enhanceTrace(trace, response);
		@SuppressWarnings("unchecked")
		Map<String, Object> map = (Map<String, Object>) ((Map<String, Object>) trace
				.get("headers")).get("response");
		assertThat(map.get("status").toString()).isEqualTo("404");
	}

	@Test
	public void filterAddsTimeTaken() throws Exception {
		WebRequestTraceFilter filter = new WebRequestTraceFilter(this.repository);
		MockHttpServletRequest request = spy(new MockHttpServletRequest("GET", "/foo"));
		MockHttpServletResponse response = new MockHttpServletResponse();
		MockFilterChain chain = new MockFilterChain();
		filter.doFilter(request, response, chain);
		String timeTaken = (String) this.repository.findAll().iterator().next().getInfo()
				.get("timeTaken");
		assertThat(timeTaken).isNotNull();
	}

	@Test
	public void filterHasError() {
		WebRequestTraceFilter filter = new WebRequestTraceFilter(this.repository);
		filter.setErrorAttributes(new DefaultErrorAttributes());
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/foo");
		MockHttpServletResponse response = new MockHttpServletResponse();
		response.setStatus(500);
		request.setAttribute("javax.servlet.error.exception",
				new IllegalStateException("Foo"));
		response.addHeader("Content-Type", "application/json");
		Map<String, Object> trace = filter.getTrace(request);
		filter.enhanceTrace(trace, response);
		@SuppressWarnings("unchecked")
		Map<String, Object> map = (Map<String, Object>) trace.get("error");
		System.err.println(map);
		assertThat(map.get("message").toString()).isEqualTo("Foo");
	}

	@Test
	@SuppressWarnings("unchecked")
	public void filterHas500ResponseStatusWhenExceptionIsThrown()
			throws ServletException, IOException {
		WebRequestTraceFilter filter = new WebRequestTraceFilter(this.repository);
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/foo");
		MockHttpServletResponse response = new MockHttpServletResponse();

		try {
			filter.doFilterInternal(request, response, (req, resp) -> {
				throw new RuntimeException();
			});
			fail("Exception was swallowed");
		}
		catch (RuntimeException ex) {
			Map<String, Object> headers = (Map<String, Object>) this.repository.findAll()
					.iterator().next().getInfo().get("headers");
			Map<String, Object> responseHeaders = (Map<String, Object>) headers
					.get("response");
			assertThat((String) responseHeaders.get("status")).isEqualTo("500");
		}
	}

	@Test
	@SuppressWarnings("unchecked")
	public void postProcessRequestHeaders() throws Exception {
		WebRequestTraceFilter filter = new WebRequestTraceFilter(this.repository) {

			@Override
			protected void postProcessRequestHeaders(Map<String, Object> headers) {
				headers.remove("Test");
			}

		};
		MockHttpServletRequest request = spy(new MockHttpServletRequest("GET", "/foo"));
		request.addHeader("Accept", "application/json");
		request.addHeader("Test", "spring");
		Map<String, Object> map = (Map<String, Object>) filter.getTrace(request)
				.get("headers");
		assertThat(map.get("request").toString()).isEqualTo("{Accept=application/json}");
	}

}
