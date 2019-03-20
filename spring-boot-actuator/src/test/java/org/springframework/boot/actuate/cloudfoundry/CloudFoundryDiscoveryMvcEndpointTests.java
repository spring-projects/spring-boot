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

package org.springframework.boot.actuate.cloudfoundry;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;

import org.springframework.boot.actuate.endpoint.AbstractEndpoint;
import org.springframework.boot.actuate.endpoint.mvc.EndpointMvcAdapter;
import org.springframework.boot.actuate.endpoint.mvc.NamedMvcEndpoint;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link CloudFoundryDiscoveryMvcEndpoint}.
 *
 * @author Madhura Bhave
 */
public class CloudFoundryDiscoveryMvcEndpointTests {

	private CloudFoundryDiscoveryMvcEndpoint endpoint;

	private Set<NamedMvcEndpoint> endpoints;

	@Before
	public void setup() {
		NamedMvcEndpoint endpoint = new TestMvcEndpoint(new TestEndpoint("a"));
		this.endpoints = new LinkedHashSet<NamedMvcEndpoint>();
		this.endpoints.add(endpoint);
		this.endpoint = new CloudFoundryDiscoveryMvcEndpoint(this.endpoints);
	}

	@Test
	public void cloudfoundryHalJsonEndpointHasEmptyPath() throws Exception {
		assertThat(this.endpoint.getPath()).isEmpty();
	}

	@Test
	public void linksResponseWhenRequestUriHasNoTrailingSlash() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest("GET",
				"/cloudfoundryapplication");
		AccessLevel.FULL.put(request);
		Map<String, CloudFoundryDiscoveryMvcEndpoint.Link> links = this.endpoint
				.links(request).get("_links");
		assertThat(links.get("self").getHref())
				.isEqualTo("http://localhost/cloudfoundryapplication");
		assertThat(links.get("a").getHref())
				.isEqualTo("http://localhost/cloudfoundryapplication/a");
	}

	@Test
	public void linksResponseWhenRequestUriHasTrailingSlash() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest("GET",
				"/cloudfoundryapplication/");
		AccessLevel.FULL.put(request);
		Map<String, CloudFoundryDiscoveryMvcEndpoint.Link> links = this.endpoint
				.links(request).get("_links");
		assertThat(links.get("self").getHref())
				.isEqualTo("http://localhost/cloudfoundryapplication");
		assertThat(links.get("a").getHref())
				.isEqualTo("http://localhost/cloudfoundryapplication/a");
	}

	@Test
	public void linksResponseWhenRequestHasAccessLevelRestricted() throws Exception {
		NamedMvcEndpoint testHealthMvcEndpoint = new TestMvcEndpoint(
				new TestEndpoint("health"));
		this.endpoints.add(testHealthMvcEndpoint);
		MockHttpServletRequest request = new MockHttpServletRequest("GET",
				"/cloudfoundryapplication/");
		AccessLevel.RESTRICTED.put(request);
		Map<String, CloudFoundryDiscoveryMvcEndpoint.Link> links = this.endpoint
				.links(request).get("_links");
		assertThat(links.get("self").getHref())
				.isEqualTo("http://localhost/cloudfoundryapplication");
		assertThat(links.get("health").getHref())
				.isEqualTo("http://localhost/cloudfoundryapplication/health");
		assertThat(links.get("a")).isNull();
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
