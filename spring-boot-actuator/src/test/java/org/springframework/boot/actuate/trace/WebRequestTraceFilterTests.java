/*
 * Copyright 2012-2014 the original author or authors.
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
import java.io.IOException;

import java.security.Principal;
import java.util.Map;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.junit.Test;
import org.springframework.boot.autoconfigure.web.DefaultErrorAttributes;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.util.ContentCachingRequestWrapper;

import static org.junit.Assert.assertEquals;

/**
 * Tests for {@link WebRequestTraceFilter}.
 *
 * @author Dave Syer
 * @author Wallace Wadge
 */
public class WebRequestTraceFilterTests {

	private final InMemoryTraceRepository inMemoryTraceRepository = new InMemoryTraceRepository();

	private final WebRequestTraceFilter filter = new WebRequestTraceFilter(this.inMemoryTraceRepository);


	@Test
	public void filterDumpsRequestResponse() throws IOException, ServletException {
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/foo");
		request.addHeader("Accept", "application/json");

		request.setContextPath("some.context.path");
		request.setContent("Hello, World!".getBytes());
		request.setRemoteAddr("some.remote.addr");
		request.setQueryString("some.query.string");
		request.setParameter("param", "paramvalue");
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


		this.filter.setIncludeClientInfo(true);
		this.filter.setIncludeContextPath(true);
		this.filter.setIncludeCookies(true);
		this.filter.setIncludeParameters(true);
		this.filter.setIncludePathInfo(true);
		this.filter.setIncludePayload(true);
		this.filter.setIncludePayloadResponse(true);
		this.filter.setIncludeQueryString(true);
		this.filter.setIncludePathTranslated(true);
		this.filter.setIncludeAuthType(true);
		this.filter.setIncludeUserPrincipal(true);
		this.filter.setIncludeUserPrincipal(true);


		this.filter.doFilterInternal(request, response, new FilterChain() {
			@Override
			public void doFilter(ServletRequest request, ServletResponse response) throws IOException, ServletException {
				BufferedReader bufferedReader = request.getReader();
				while (bufferedReader.readLine() != null) {
					// read the contents as normal (forces cache to fill up)
				}
				response.getWriter().println("Goodbye, World!");
			}
		});


		assertEquals(1, this.inMemoryTraceRepository.findAll().size());
		Map<String, Object> trace = this.inMemoryTraceRepository.findAll().iterator().next().getInfo();
		@SuppressWarnings("unchecked")
		Map<String, Object> map = (Map<String, Object>) trace.get("headers");
		assertEquals("{Content-Type=application/json, status=200}", map.get("response")
				.toString());

		assertEquals("GET", trace.get("method"));
		assertEquals("/foo", trace.get("path"));
		assertEquals("paramvalue", ((String[]) ((Map) trace.get("requestParams")).get("param"))[0]);
		assertEquals("some.remote.addr", trace.get("requestRemoteAddr"));
		assertEquals("some.query.string", trace.get("requestQueryString"));
		assertEquals(principal.getName(), ((Principal) trace.get("userPrincipal")).getName());
		assertEquals("some.context.path", trace.get("contextPath"));
		assertEquals("Hello, World!", trace.get(WebRequestTraceFilter.TRACE_RQ_KEY));
		assertEquals("authType", trace.get(WebRequestTraceFilter.TRACE_RQ_AUTH_TYPE));
		assertEquals("Goodbye, World!", trace.get(WebRequestTraceFilter.TRACE_RESP_KEY));
		assertEquals("{Accept=application/json}", map.get("request").toString());

	}

	@Test
	public void filterHasResponseStatus() {
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/foo");
		MockHttpServletResponse response = new MockHttpServletResponse();
		response.setStatus(404);
		response.addHeader("Content-Type", "application/json");
		Map<String, Object> trace = this.filter.getTrace(request, this.filter.createMessage(new ContentCachingRequestWrapper(request), null, null));
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
		request.setAttribute("javax.servlet.error.exception", new IllegalStateException(
				"Foo"));
		response.addHeader("Content-Type", "application/json");
		Map<String, Object> trace = this.filter.getTrace(request, this.filter.createMessage(new ContentCachingRequestWrapper(request), null, null));
		this.filter.enhanceTrace(trace, response);
		@SuppressWarnings("unchecked")
		Map<String, Object> map = (Map<String, Object>) trace.get("error");
		System.err.println(map);
		assertEquals("Foo", map.get("message").toString());
	}
}
