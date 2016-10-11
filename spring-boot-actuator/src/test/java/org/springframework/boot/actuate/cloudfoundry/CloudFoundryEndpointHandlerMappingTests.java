/*
 * Copyright 2012-2016 the original author or authors.
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

package org.springframework.boot.actuate.cloudfoundry;

import java.util.Arrays;

import org.junit.Test;

import org.springframework.boot.actuate.endpoint.AbstractEndpoint;
import org.springframework.boot.actuate.endpoint.mvc.EndpointMvcAdapter;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.servlet.HandlerExecutionChain;
import org.springframework.web.servlet.HandlerInterceptor;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link CloudFoundryEndpointHandlerMapping}.
 *
 * @author Madhura Bhave
 */
public class CloudFoundryEndpointHandlerMappingTests {

	@Test
	public void getHandlerExecutionChainShouldHaveSecurityInterceptor() throws Exception {
		TestMvcEndpoint endpoint = new TestMvcEndpoint(new TestEndpoint("a"));
		CloudFoundryEndpointHandlerMapping handlerMapping = new CloudFoundryEndpointHandlerMapping(
				Arrays.asList(endpoint));
		HandlerExecutionChain handlerExecutionChain = handlerMapping
				.getHandlerExecutionChain(endpoint, new MockHttpServletRequest());
		HandlerInterceptor[] interceptors = handlerExecutionChain.getInterceptors();
		assertThat(interceptors).hasAtLeastOneElementOfType(
				CloudFoundryEndpointHandlerMapping.SecurityInterceptor.class);
	}

	@Test
	public void getHandlerExecutionChainWhenEndpointHasPathShouldMapAgainstName()
			throws Exception {
		TestMvcEndpoint testMvcEndpoint = new TestMvcEndpoint(new TestEndpoint("a"));
		testMvcEndpoint.setPath("something-else");
		CloudFoundryEndpointHandlerMapping handlerMapping = new CloudFoundryEndpointHandlerMapping(
				Arrays.asList(testMvcEndpoint));
		assertThat(handlerMapping.getPath(testMvcEndpoint)).isEqualTo("/a");
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

}
