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

import java.lang.reflect.Method;

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

	private StaticApplicationContext context = new StaticApplicationContext();
	private EndpointHandlerMapping mapping = new EndpointHandlerMapping();
	private Method method;

	@Before
	public void init() throws Exception {
		this.context.getDefaultListableBeanFactory().registerSingleton("mapping",
				this.mapping);
		this.mapping.setApplicationContext(this.context);
		this.method = ReflectionUtils.findMethod(TestEndpoint.class, "invoke");
	}

	@Test
	public void withoutPrefix() throws Exception {
		TestEndpoint endpointA = new TestEndpoint("/a");
		TestEndpoint endpointB = new TestEndpoint("/b");
		this.context.getDefaultListableBeanFactory().registerSingleton(
				endpointA.getPath(), endpointA);
		this.context.getDefaultListableBeanFactory().registerSingleton(
				endpointB.getPath(), endpointB);
		this.mapping.afterPropertiesSet();
		assertThat(this.mapping.getHandler(new MockHttpServletRequest("GET", "/a"))
				.getHandler(),
				equalTo((Object) new HandlerMethod(endpointA, this.method)));
		assertThat(this.mapping.getHandler(new MockHttpServletRequest("GET", "/b"))
				.getHandler(),
				equalTo((Object) new HandlerMethod(endpointB, this.method)));
		assertThat(this.mapping.getHandler(new MockHttpServletRequest("GET", "/c")),
				nullValue());
	}

	@Test
	public void withPrefix() throws Exception {
		TestEndpoint endpointA = new TestEndpoint("/a");
		TestEndpoint endpointB = new TestEndpoint("/b");
		this.context.getDefaultListableBeanFactory().registerSingleton(
				endpointA.getPath(), endpointA);
		this.context.getDefaultListableBeanFactory().registerSingleton(
				endpointB.getPath(), endpointB);
		this.mapping.setPrefix("/a");
		this.mapping.afterPropertiesSet();
		assertThat(this.mapping.getHandler(new MockHttpServletRequest("GET", "/a/a"))
				.getHandler(),
				equalTo((Object) new HandlerMethod(endpointA, this.method)));
		assertThat(this.mapping.getHandler(new MockHttpServletRequest("GET", "/a/b"))
				.getHandler(),
				equalTo((Object) new HandlerMethod(endpointB, this.method)));
		assertThat(this.mapping.getHandler(new MockHttpServletRequest("GET", "/a")),
				nullValue());
	}

	@Test(expected = HttpRequestMethodNotSupportedException.class)
	public void onlyGetHttpMethodForNonActionEndpoints() throws Exception {
		TestEndpoint endpoint = new TestEndpoint("/a");
		this.context.getDefaultListableBeanFactory().registerSingleton(
				endpoint.getPath(), endpoint);
		this.mapping.afterPropertiesSet();
		assertNotNull(this.mapping.getHandler(new MockHttpServletRequest("GET", "/a")));
		assertNull(this.mapping.getHandler(new MockHttpServletRequest("POST", "/a")));
	}

	@Test
	public void postHttpMethodForActionEndpoints() throws Exception {
		TestEndpoint endpoint = new TestActionEndpoint("/a");
		this.context.getDefaultListableBeanFactory().registerSingleton(
				endpoint.getPath(), endpoint);
		this.mapping.afterPropertiesSet();
		assertNotNull(this.mapping.getHandler(new MockHttpServletRequest("POST", "/a")));
	}

	@Test(expected = HttpRequestMethodNotSupportedException.class)
	public void onlyPostHttpMethodForActionEndpoints() throws Exception {
		TestEndpoint endpoint = new TestActionEndpoint("/a");
		this.context.getDefaultListableBeanFactory().registerSingleton(
				endpoint.getPath(), endpoint);
		this.mapping.afterPropertiesSet();
		assertNotNull(this.mapping.getHandler(new MockHttpServletRequest("POST", "/a")));
		assertNull(this.mapping.getHandler(new MockHttpServletRequest("GET", "/a")));
	}

	@Test
	public void disabled() throws Exception {
		TestEndpoint endpoint = new TestEndpoint("/a");
		this.context.getDefaultListableBeanFactory().registerSingleton(
				endpoint.getPath(), endpoint);
		this.mapping.setDisabled(true);
		this.mapping.afterPropertiesSet();
		assertThat(this.mapping.getHandler(new MockHttpServletRequest("GET", "/a")),
				nullValue());
	}

	@FrameworkEndpoint
	private static class TestEndpoint extends AbstractEndpoint<Object> {

		public TestEndpoint(String path) {
			super(path);
		}

		@Override
		@RequestMapping(method = RequestMethod.GET)
		public Object invoke() {
			return null;
		}

	}

	@FrameworkEndpoint
	private static class TestActionEndpoint extends TestEndpoint {

		public TestActionEndpoint(String path) {
			super(path);
		}

		@Override
		@RequestMapping(method = RequestMethod.POST)
		public Object invoke() {
			return null;
		}
	}

}
