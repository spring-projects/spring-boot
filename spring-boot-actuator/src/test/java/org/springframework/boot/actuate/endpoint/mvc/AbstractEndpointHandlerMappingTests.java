/*
 * Copyright 2012-2017 the original author or authors.
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

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import org.junit.Test;

import org.springframework.boot.actuate.endpoint.AbstractEndpoint;
import org.springframework.context.support.StaticApplicationContext;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.HandlerInterceptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link AbstractEndpointHandlerMapping}.
 *
 * @author Madhura Bhave
 */
public abstract class AbstractEndpointHandlerMappingTests {

	private final StaticApplicationContext context = new StaticApplicationContext();

	@Test
	public void securityInterceptorShouldBePresentForNonCorsRequest() throws Exception {
		HandlerInterceptor securityInterceptor = mock(HandlerInterceptor.class);
		TestActionEndpoint endpoint = new TestActionEndpoint(new TestEndpoint("a"));
		AbstractEndpointHandlerMapping<?> mapping = new TestEndpointHandlerMapping<TestActionEndpoint>(
				Collections.singletonList(endpoint));
		mapping.setApplicationContext(this.context);
		mapping.setSecurityInterceptor(securityInterceptor);
		mapping.afterPropertiesSet();
		assertThat(mapping.getHandler(request("POST", "/a")).getInterceptors())
				.contains(securityInterceptor);
	}

	@Test
	public void securityInterceptorIfNullShouldNotBeAdded() throws Exception {
		TestActionEndpoint endpoint = new TestActionEndpoint(new TestEndpoint("a"));
		AbstractEndpointHandlerMapping<?> mapping = new TestEndpointHandlerMapping<TestActionEndpoint>(
				Collections.singletonList(endpoint));
		mapping.setApplicationContext(this.context);
		mapping.afterPropertiesSet();
		assertThat(mapping.getHandler(request("POST", "/a")).getInterceptors())
				.hasSize(1);
	}

	@Test
	public void securityInterceptorShouldBePresentAfterCorsInterceptorForCorsRequest()
			throws Exception {
		HandlerInterceptor securityInterceptor = mock(HandlerInterceptor.class);
		TestActionEndpoint endpoint = new TestActionEndpoint(new TestEndpoint("a"));
		AbstractEndpointHandlerMapping<?> mapping = new TestEndpointHandlerMapping<TestActionEndpoint>(
				Collections.singletonList(endpoint));
		mapping.setApplicationContext(this.context);
		mapping.setSecurityInterceptor(securityInterceptor);
		mapping.afterPropertiesSet();
		MockHttpServletRequest request = request("POST", "/a");
		request.addHeader("Origin", "http://example.com");
		assertThat(mapping.getHandler(request).getInterceptors().length).isEqualTo(3);
		assertThat(mapping.getHandler(request).getInterceptors()[2])
				.isEqualTo(securityInterceptor);
	}

	@Test
	public void pathNotMappedWhenGetPathReturnsNull() throws Exception {
		TestMvcEndpoint endpoint = new TestMvcEndpoint(new TestEndpoint("a"));
		TestActionEndpoint other = new TestActionEndpoint(new TestEndpoint("b"));
		AbstractEndpointHandlerMapping<?> mapping = new TestEndpointHandlerMapping<MvcEndpoint>(
				Arrays.<MvcEndpoint>asList(endpoint, other));
		mapping.setApplicationContext(this.context);
		mapping.afterPropertiesSet();
		assertThat(mapping.getHandlerMethods()).hasSize(1);
		assertThat(mapping.getHandler(request("GET", "/a"))).isNull();
		assertThat(mapping.getHandler(request("POST", "/b"))).isNotNull();
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

	private static class TestEndpointHandlerMapping<E extends MvcEndpoint>
			extends AbstractEndpointHandlerMapping<E> {

		TestEndpointHandlerMapping(Collection<E> endpoints) {
			super(endpoints);
		}

		@Override
		protected String getPath(MvcEndpoint endpoint) {
			if (endpoint instanceof TestActionEndpoint) {
				return super.getPath(endpoint);
			}
			return null;
		}

	}

}
