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

package org.springframework.boot.actuate.endpoint.mvc;

import java.lang.reflect.Method;
import java.util.Arrays;

import org.junit.Before;
import org.junit.Test;
import org.springframework.boot.actuate.endpoint.AbstractEndpoint;
import org.springframework.context.support.StaticApplicationContext;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.util.ReflectionUtils;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.method.HandlerMethod;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;

/**
 * Tests for {@link EndpointHandlerMapping}.
 * 
 * @author Phillip Webb
 * @author Dave Syer
 */
public class EndpointHandlerMappingTests {

	private final StaticApplicationContext context = new StaticApplicationContext();
	private Method method;

	@Before
	public void init() throws Exception {
		this.method = ReflectionUtils.findMethod(TestMvcEndpoint.class, "invoke");
	}

	@Test
	public void withoutPrefix() throws Exception {
		TestMvcEndpoint endpointA = new TestMvcEndpoint(new TestEndpoint("/a"));
		TestMvcEndpoint endpointB = new TestMvcEndpoint(new TestEndpoint("/b"));
		EndpointHandlerMapping mapping = new EndpointHandlerMapping(Arrays.asList(
				endpointA, endpointB));
		mapping.setApplicationContext(this.context);
		mapping.afterPropertiesSet();
		assertThat(mapping.getHandler(new MockHttpServletRequest("GET", "/a"))
				.getHandler(),
				equalTo((Object) new HandlerMethod(endpointA, this.method)));
		assertThat(mapping.getHandler(new MockHttpServletRequest("GET", "/b"))
				.getHandler(),
				equalTo((Object) new HandlerMethod(endpointB, this.method)));
		assertThat(mapping.getHandler(new MockHttpServletRequest("GET", "/c")),
				nullValue());
	}

	@Test
	public void withPrefix() throws Exception {
		TestMvcEndpoint endpointA = new TestMvcEndpoint(new TestEndpoint("/a"));
		TestMvcEndpoint endpointB = new TestMvcEndpoint(new TestEndpoint("/b"));
		EndpointHandlerMapping mapping = new EndpointHandlerMapping(Arrays.asList(
				endpointA, endpointB));
		mapping.setApplicationContext(this.context);
		mapping.setPrefix("/a");
		mapping.afterPropertiesSet();
		assertThat(mapping.getHandler(new MockHttpServletRequest("GET", "/a/a"))
				.getHandler(),
				equalTo((Object) new HandlerMethod(endpointA, this.method)));
		assertThat(mapping.getHandler(new MockHttpServletRequest("GET", "/a/b"))
				.getHandler(),
				equalTo((Object) new HandlerMethod(endpointB, this.method)));
		assertThat(mapping.getHandler(new MockHttpServletRequest("GET", "/a")),
				nullValue());
	}

	@Test(expected = HttpRequestMethodNotSupportedException.class)
	public void onlyGetHttpMethodForNonActionEndpoints() throws Exception {
		TestActionEndpoint endpoint = new TestActionEndpoint(new TestEndpoint("/a"));
		EndpointHandlerMapping mapping = new EndpointHandlerMapping(
				Arrays.asList(endpoint));
		mapping.setApplicationContext(this.context);
		mapping.afterPropertiesSet();
		assertNotNull(mapping.getHandler(new MockHttpServletRequest("GET", "/a")));
		assertNull(mapping.getHandler(new MockHttpServletRequest("POST", "/a")));
	}

	@Test
	public void postHttpMethodForActionEndpoints() throws Exception {
		TestActionEndpoint endpoint = new TestActionEndpoint(new TestEndpoint("/a"));
		EndpointHandlerMapping mapping = new EndpointHandlerMapping(
				Arrays.asList(endpoint));
		mapping.setApplicationContext(this.context);
		mapping.afterPropertiesSet();
		assertNotNull(mapping.getHandler(new MockHttpServletRequest("POST", "/a")));
	}

	@Test(expected = HttpRequestMethodNotSupportedException.class)
	public void onlyPostHttpMethodForActionEndpoints() throws Exception {
		TestActionEndpoint endpoint = new TestActionEndpoint(new TestEndpoint("/a"));
		EndpointHandlerMapping mapping = new EndpointHandlerMapping(
				Arrays.asList(endpoint));
		mapping.setApplicationContext(this.context);
		mapping.afterPropertiesSet();
		assertNotNull(mapping.getHandler(new MockHttpServletRequest("POST", "/a")));
		assertNull(mapping.getHandler(new MockHttpServletRequest("GET", "/a")));
	}

	@Test
	public void disabled() throws Exception {
		TestMvcEndpoint endpoint = new TestMvcEndpoint(new TestEndpoint("/a"));
		EndpointHandlerMapping mapping = new EndpointHandlerMapping(
				Arrays.asList(endpoint));
		mapping.setDisabled(true);
		mapping.setApplicationContext(this.context);
		mapping.afterPropertiesSet();
		assertThat(mapping.getHandler(new MockHttpServletRequest("GET", "/a")),
				nullValue());
	}

	private static class TestEndpoint extends AbstractEndpoint<Object> {

		public TestEndpoint(String path) {
			super(path);
		}

		@Override
		public Object invoke() {
			return null;
		}

	}

	private static class TestMvcEndpoint extends EndpointMvcAdapter {

		public TestMvcEndpoint(TestEndpoint delegate) {
			super(delegate);
		}

	}

	private static class TestActionEndpoint extends EndpointMvcAdapter {

		public TestActionEndpoint(TestEndpoint delegate) {
			super(delegate);
		}

		@Override
		@RequestMapping(method = RequestMethod.POST)
		public Object invoke() {
			return null;
		}

	}

}
