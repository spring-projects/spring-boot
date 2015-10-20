/*
 * Copyright 2012-2015 the original author or authors.
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

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.Cookie;

import org.junit.Test;

import org.springframework.boot.actuate.trace.TraceProperties.Include;
import org.springframework.boot.autoconfigure.web.DefaultErrorAttributes;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Tests for {@link WebRequestTraceFilter}.
 *
 * @author Dave Syer
 * @author Wallace Wadge
 * @author Phillip Webb
 */
public class WebRequestTraceFilterTests {

	private final InMemoryTraceRepository repository = new InMemoryTraceRepository();

	private TraceProperties properties = new TraceProperties();

	private WebRequestTraceFilter filter = new WebRequestTraceFilter(this.repository,
			this.properties);

	@Test
	@SuppressWarnings("unchecked")
	public void filterAddsTraceWithDefaultIncludes() {
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/foo");
		request.addHeader("Accept", "application/json");
		Map<String, Object> trace = this.filter.getTrace(request);
		assertEquals("GET", trace.get("method"));
		assertEquals("/foo", trace.get("path"));
		Map<String, Object> map = (Map<String, Object>) trace.get("headers");
		assertEquals("{Accept=application/json}", map.get("request").toString());
	}

	@Test
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public void filterAddsTraceWithCustomIncludes() throws IOException, ServletException {
		this.properties.setInclude(EnumSet.allOf(Include.class));
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/foo");
		request.addHeader("Accept", "application/json");
		request.setContextPath("some.context.path");
		request.setContent("Hello, World!".getBytes());
		request.setRemoteAddr("some.remote.addr");
		request.setQueryString("some.query.string");
		request.setParameter("param", "paramvalue");
		File tmp = File.createTempFile("spring-boot", "tmp");
		String url = tmp.toURI().toURL().toString();
		request.setPathInfo(url);
		tmp.deleteOnExit();
		Cookie cookie = new Cookie("testCookie", "testValue");
		request.setCookies(cookie);
		request.setAuthType("authType");
		Principal principal = new Principal() {

			@Override
			public String getName() {
				return "principalTest";
			}

		};
		request.setUserPrincipal(principal);
		MockHttpServletResponse response = new MockHttpServletResponse();
		response.addHeader("Content-Type", "application/json");
		this.filter.doFilterInternal(request, response, new FilterChain() {

			@Override
			public void doFilter(ServletRequest request, ServletResponse response)
					throws IOException, ServletException {
				BufferedReader bufferedReader = request.getReader();
				while (bufferedReader.readLine() != null) {
					// read the contents as normal (forces cache to fill up)
				}
				response.getWriter().println("Goodbye, World!");
			}

		});
		assertEquals(1, this.repository.findAll().size());
		Map<String, Object> trace = this.repository.findAll().iterator().next().getInfo();
		Map<String, Object> map = (Map<String, Object>) trace.get("headers");
		assertEquals("{Content-Type=application/json, status=200}",
				map.get("response").toString());
		assertEquals("GET", trace.get("method"));
		assertEquals("/foo", trace.get("path"));
		assertEquals("paramvalue",
				((String[]) ((Map) trace.get("parameters")).get("param"))[0]);
		assertEquals("some.remote.addr", trace.get("remoteAddress"));
		assertEquals("some.query.string", trace.get("query"));
		assertEquals(principal.getName(), trace.get("userPrincipal"));
		assertEquals("some.context.path", trace.get("contextPath"));
		assertEquals(url, trace.get("pathInfo"));
		assertEquals("authType", trace.get("authType"));
		assertEquals("{Accept=application/json}", map.get("request").toString());
	}

	@Test
	@SuppressWarnings({ "unchecked" })
	public void filterDoesNotAddResponseHeadersWithoutResponseHeadersInclude()
			throws ServletException, IOException {
		this.properties.setInclude(Collections.singleton(Include.REQUEST_HEADERS));
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/foo");
		MockHttpServletResponse response = new MockHttpServletResponse();
		response.addHeader("Content-Type", "application/json");
		this.filter.doFilterInternal(request, response, new FilterChain() {
			@Override
			public void doFilter(ServletRequest request, ServletResponse response)
					throws IOException, ServletException {
			}
		});
		Map<String, Object> info = this.repository.findAll().iterator().next().getInfo();
		Map<String, Object> headers = (Map<String, Object>) info.get("headers");
		assertTrue(headers.get("response") == null);
	}

	@Test
	public void filterHasResponseStatus() {
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/foo");
		MockHttpServletResponse response = new MockHttpServletResponse();
		response.setStatus(404);
		response.addHeader("Content-Type", "application/json");
		Map<String, Object> trace = this.filter.getTrace(request);
		this.filter.enhanceTrace(trace, response);
		@SuppressWarnings("unchecked")
		Map<String, Object> map = (Map<String, Object>) ((Map<String, Object>) trace
				.get("headers")).get("response");
		assertEquals("404", map.get("status").toString());
	}

	@Test
	public void filterHasError() {
		this.filter.setErrorAttributes(new DefaultErrorAttributes());
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/foo");
		MockHttpServletResponse response = new MockHttpServletResponse();
		response.setStatus(500);
		request.setAttribute("javax.servlet.error.exception",
				new IllegalStateException("Foo"));
		response.addHeader("Content-Type", "application/json");
		Map<String, Object> trace = this.filter.getTrace(request);
		this.filter.enhanceTrace(trace, response);
		@SuppressWarnings("unchecked")
		Map<String, Object> map = (Map<String, Object>) trace.get("error");
		System.err.println(map);
		assertEquals("Foo", map.get("message").toString());
	}

}
