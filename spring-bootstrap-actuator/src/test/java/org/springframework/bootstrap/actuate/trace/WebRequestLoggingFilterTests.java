/*
 * Copyright 2012-2013 the original author or authors.
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
package org.springframework.bootstrap.actuate.trace;

import java.util.Map;

import org.junit.Test;
import org.springframework.bootstrap.actuate.trace.InMemoryTraceRepository;
import org.springframework.bootstrap.actuate.trace.WebRequestLoggingFilter;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.junit.Assert.assertEquals;

/**
 * @author Dave Syer
 * 
 */
public class WebRequestLoggingFilterTests {

	private WebRequestLoggingFilter filter = new WebRequestLoggingFilter(
			new InMemoryTraceRepository());

	@Test
	public void filterDumpsRequest() {
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/foo");
		request.addHeader("Accept", "application/json");
		Map<String, Object> trace = this.filter.getTrace(request);
		assertEquals("GET", trace.get("method"));
		assertEquals("/foo", trace.get("path"));
		assertEquals("{Accept=application/json}", trace.get("headers").toString());
	}
}
