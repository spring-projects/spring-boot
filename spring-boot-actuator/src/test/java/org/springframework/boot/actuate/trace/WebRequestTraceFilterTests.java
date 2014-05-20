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

import java.util.Map;

import org.junit.Test;
import org.springframework.boot.autoconfigure.web.DefaultErrorAttributes;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.Assert.assertEquals;

/**
 * Tests for {@link WebRequestTraceFilter}.
 * 
 * @author Dave Syer
 */
public class WebRequestTraceFilterTests {

	private final WebRequestTraceFilter filter = new WebRequestTraceFilter(
			new InMemoryTraceRepository());

	@Test
	public void filterDumpsRequest() {
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/foo");
		request.addHeader("Accept", "application/json");
		Map<String, Object> trace = this.filter.getTrace(request);
		assertEquals("GET", trace.get("method"));
		assertEquals("/foo", trace.get("path"));
		@SuppressWarnings("unchecked")
		Map<String, Object> map = (Map<String, Object>) trace.get("headers");
		assertEquals("{Accept=application/json}", map.get("request").toString());
	}

	@Test
	public void filterDumpsResponse() {
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/foo");
		MockHttpServletResponse response = new MockHttpServletResponse();
		response.addHeader("Content-Type", "application/json");
		Map<String, Object> trace = this.filter.getTrace(request);
		this.filter.enhanceTrace(trace, response);
		@SuppressWarnings("unchecked")
		Map<String, Object> map = (Map<String, Object>) trace.get("headers");
		assertEquals("{Content-Type=application/json, status=200}", map.get("response")
				.toString());
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
		request.setAttribute("javax.servlet.error.exception", new IllegalStateException(
				"Foo"));
		response.addHeader("Content-Type", "application/json");
		Map<String, Object> trace = this.filter.getTrace(request);
		this.filter.enhanceTrace(trace, response);
		@SuppressWarnings("unchecked")
		Map<String, Object> map = (Map<String, Object>) trace.get("error");
		System.err.println(map);
		assertEquals("Foo", map.get("message").toString());
	}
}
