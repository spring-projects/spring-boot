/*
 * Copyright 2012-2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.method.HandlerMethod;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link EndpointHandlerMapping}.
 *
 * @author Phillip Webb
 * @author Dave Syer
 */
public class EndpointHandlerMappingTests extends AbstractEndpointHandlerMappingTests {

	private final StaticApplicationContext context = new StaticApplicationContext();

	private Method method;

	@Before
	public void init() throws Exception {
		this.method = ReflectionUtils.findMethod(TestMvcEndpoint.class, "invoke");
	}

	@Test
	public void withoutPrefix() throws Exception {
		TestMvcEndpoint endpointA = new TestMvcEndpoint(new TestEndpoint("a"));
		TestMvcEndpoint endpointB = new TestMvcEndpoint(new TestEndpoint("b"));
		EndpointHandlerMapping mapping = new EndpointHandlerMapping(
				Arrays.asList(endpointA, endpointB));
		mapping.setApplicationContext(this.context);
		mapping.afterPropertiesSet();
		assertThat(mapping.getHandler(request("GET", "/a")).getHandler())
				.isEqualTo(new HandlerMethod(endpointA, this.method));
		assertThat(mapping.getHandler(request("GET", "/b")).getHandler())
				.isEqualTo(new HandlerMethod(endpointB, this.method));
		assertThat(mapping.getHandler(request("GET", "/c"))).isNull();
	}

	@Test
	public void withPrefix() throws Exception {
		TestMvcEndpoint endpointA = new TestMvcEndpoint(new TestEndpoint("a"));
		TestMvcEndpoint endpointB = new TestMvcEndpoint(new TestEndpoint("b"));
		EndpointHandlerMapping mapping = new EndpointHandlerMapping(
				Arrays.asList(endpointA, endpointB));
		mapping.setApplicationContext(this.context);
		mapping.setPrefix("/a");
		mapping.afterPropertiesSet();
		assertThat(mapping.getHandler(new MockHttpServletRequest("GET", "/a/a"))
				.getHandler()).isEqualTo(new HandlerMethod(endpointA, this.method));
		assertThat(mapping.getHandler(new MockHttpServletRequest("GET", "/a/b"))
				.getHandler()).isEqualTo(new HandlerMethod(endpointB, this.method));
		assertThat(mapping.getHandler(request("GET", "/a"))).isNull();
	}

	@Test(expected = HttpRequestMethodNotSupportedException.class)
	public void onlyGetHttpMethodForNonActionEndpoints() throws Exception {
		TestActionEndpoint endpoint = new TestActionEndpoint(new TestEndpoint("a"));
		EndpointHandlerMapping mapping = new EndpointHandlerMapping(
				Arrays.asList(endpoint));
		mapping.setApplicationContext(this.context);
		mapping.afterPropertiesSet();
		assertThat(mapping.getHandler(request("GET", "/a"))).isNotNull();
		assertThat(mapping.getHandler(request("POST", "/a"))).isNull();
	}

	@Test
	public void postHttpMethodForActionEndpoints() throws Exception {
		TestActionEndpoint endpoint = new TestActionEndpoint(new TestEndpoint("a"));
		EndpointHandlerMapping mapping = new EndpointHandlerMapping(
				Arrays.asList(endpoint));
		mapping.setApplicationContext(this.context);
		mapping.afterPropertiesSet();
		assertThat(mapping.getHandler(request("POST", "/a"))).isNotNull();
	}

	@Test(expected = HttpRequestMethodNotSupportedException.class)
	public void onlyPostHttpMethodForActionEndpoints() throws Exception {
		TestActionEndpoint endpoint = new TestActionEndpoint(new TestEndpoint("a"));
		EndpointHandlerMapping mapping = new EndpointHandlerMapping(
				Arrays.asList(endpoint));
		mapping.setApplicationContext(this.context);
		mapping.afterPropertiesSet();
		assertThat(mapping.getHandler(request("POST", "/a"))).isNotNull();
		assertThat(mapping.getHandler(request("GET", "/a"))).isNull();
	}

	@Test
	public void disabled() throws Exception {
		TestMvcEndpoint endpoint = new TestMvcEndpoint(new TestEndpoint("a"));
		EndpointHandlerMapping mapping = new EndpointHandlerMapping(
				Arrays.asList(endpoint));
		mapping.setDisabled(true);
		mapping.setApplicationContext(this.context);
		mapping.afterPropertiesSet();
		assertThat(mapping.getHandler(request("GET", "/a"))).isNull();
	}

	@Test
	public void duplicatePath() throws Exception {
		TestMvcEndpoint endpoint = new TestMvcEndpoint(new TestEndpoint("a"));
		TestActionEndpoint other = new TestActionEndpoint(new TestEndpoint("a"));
		EndpointHandlerMapping mapping = new EndpointHandlerMapping(
				Arrays.asList(endpoint, other));
		mapping.setDisabled(true);
		mapping.setApplicationContext(this.context);
		mapping.afterPropertiesSet();
		assertThat(mapping.getHandler(request("GET", "/a"))).isNull();
		assertThat(mapping.getHandler(request("POST", "/a"))).isNull();
	}

	private MockHttpServletRequest request(String method, String requestURI) {
		return new MockHttpServletRequest(method, requestURI);
	}

	private static class TestEndpoint extends AbstractEndpoint<Object> {

		TestEndpoint(String id) {
			super(id);
		}

		@Override
		public Object invoke() {
			return null;
		}

	}

	private static class TestMvcEndpoint extends EndpointMvcAdapter {

		TestMvcEndpoint(TestEndpoint delegate) {
			super(delegate);
		}

	}

	private static class TestActionEndpoint extends EndpointMvcAdapter {

		TestActionEndpoint(TestEndpoint delegate) {
			super(delegate);
		}

		@Override
		@PostMapping
		public Object invoke() {
			return null;
		}

	}

}
