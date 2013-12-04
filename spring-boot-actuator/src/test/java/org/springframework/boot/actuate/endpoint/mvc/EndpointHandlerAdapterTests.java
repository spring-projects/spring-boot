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

package org.springframework.boot.actuate.endpoint.mvc;

import java.util.Collections;
import java.util.Map;

import org.junit.Test;
import org.springframework.boot.actuate.endpoint.AbstractEndpoint;
import org.springframework.boot.actuate.endpoint.Endpoint;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link EndpointHandlerAdapter}.
 * 
 * @author Phillip Webb
 */
public class EndpointHandlerAdapterTests {

	private EndpointHandlerAdapter adapter = new EndpointHandlerAdapter();
	private MockHttpServletRequest request = new MockHttpServletRequest();
	private MockHttpServletResponse response = new MockHttpServletResponse();

	@Test
	public void onlySupportsEndpoints() throws Exception {
		assertTrue(this.adapter.supports(mock(Endpoint.class)));
		assertFalse(this.adapter.supports(mock(Object.class)));
	}

	@Test
	public void rendersJson() throws Exception {
		this.adapter.handle(this.request, this.response,
				new AbstractEndpoint<Map<String, String>>("/foo") {
					@Override
					protected Map<String, String> doInvoke() {
						return Collections.singletonMap("hello", "world");
					}
				});
		assertEquals("{\"hello\":\"world\"}", this.response.getContentAsString());
	}

	@Test
	public void rendersString() throws Exception {
		this.request.addHeader("Accept", "text/plain");
		this.adapter.handle(this.request, this.response, new AbstractEndpoint<String>(
				"/foo") {
			@Override
			protected String doInvoke() {
				return "hello world";
			}
		});
		assertEquals("hello world", this.response.getContentAsString());
	}
}
