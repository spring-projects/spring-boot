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

import java.util.Arrays;

import org.junit.Test;
import org.springframework.boot.actuate.endpoint.AbstractEndpoint;
import org.springframework.http.HttpMethod;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;

/**
 * Tests for {@link EndpointHandlerMapping}.
 * 
 * @author Phillip Webb
 */
public class EndpointHandlerMappingTests {

	@Test
	public void withoutPrefix() throws Exception {
		TestEndpoint endpointA = new TestEndpoint("/a");
		TestEndpoint endpointB = new TestEndpoint("/b");
		EndpointHandlerMapping mapping = new EndpointHandlerMapping(Arrays.asList(
				endpointA, endpointB));
		mapping.afterPropertiesSet();
		assertThat(mapping.getHandler(new MockHttpServletRequest("GET", "/a"))
				.getHandler(), equalTo((Object) endpointA));
		assertThat(mapping.getHandler(new MockHttpServletRequest("GET", "/b"))
				.getHandler(), equalTo((Object) endpointB));
		assertThat(mapping.getHandler(new MockHttpServletRequest("GET", "/c")),
				nullValue());
	}

	@Test
	public void withPrefix() throws Exception {
		TestEndpoint endpointA = new TestEndpoint("/a");
		TestEndpoint endpointB = new TestEndpoint("/b");
		EndpointHandlerMapping mapping = new EndpointHandlerMapping(Arrays.asList(
				endpointA, endpointB));
		mapping.setPrefix("/a");
		mapping.afterPropertiesSet();
		assertThat(mapping.getHandler(new MockHttpServletRequest("GET", "/a/a"))
				.getHandler(), equalTo((Object) endpointA));
		assertThat(mapping.getHandler(new MockHttpServletRequest("GET", "/a/b"))
				.getHandler(), equalTo((Object) endpointB));
		assertThat(mapping.getHandler(new MockHttpServletRequest("GET", "/a")),
				nullValue());
	}

	@Test
	public void onlyGetHttpMethodForNonActionEndpoints() throws Exception {
		TestEndpoint endpoint = new TestEndpoint("/a");
		EndpointHandlerMapping mapping = new EndpointHandlerMapping(
				Arrays.asList(endpoint));
		mapping.afterPropertiesSet();
		assertNotNull(mapping.getHandler(new MockHttpServletRequest("GET", "/a")));
		assertNull(mapping.getHandler(new MockHttpServletRequest("POST", "/a")));
	}

	@Test
	public void onlyPostHttpMethodForActionEndpoints() throws Exception {
		TestEndpoint endpoint = new TestActionEndpoint("/a");
		EndpointHandlerMapping mapping = new EndpointHandlerMapping(
				Arrays.asList(endpoint));
		mapping.afterPropertiesSet();
		assertNull(mapping.getHandler(new MockHttpServletRequest("GET", "/a")));
		assertNotNull(mapping.getHandler(new MockHttpServletRequest("POST", "/a")));
	}

	@Test
	public void disabled() throws Exception {
		TestEndpoint endpointA = new TestEndpoint("/a");
		EndpointHandlerMapping mapping = new EndpointHandlerMapping(
				Arrays.asList(endpointA));
		mapping.setDisabled(true);
		mapping.afterPropertiesSet();
		assertThat(mapping.getHandler(new MockHttpServletRequest("GET", "/a")),
				nullValue());
	}

	private static class TestEndpoint extends AbstractEndpoint<Object> {

		public TestEndpoint(String path) {
			super(path);
		}

		@Override
		public Object doInvoke() {
			return null;
		}

	}

	private static class TestActionEndpoint extends TestEndpoint {

		public TestActionEndpoint(String path) {
			super(path);
		}

		@Override
		public HttpMethod[] methods() {
			return POST_HTTP_METHOD;
		}
	}

}
