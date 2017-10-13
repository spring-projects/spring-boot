/*
 * Copyright 2012-2017 the original author or authors.
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

package org.springframework.boot.actuate.autoconfigure.endpoint.web;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import org.springframework.boot.actuate.endpoint.EndpointDiscoverer;
import org.springframework.boot.actuate.endpoint.EndpointInfo;
import org.springframework.boot.actuate.endpoint.web.WebOperation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

/**
 * Tests for {@link DefaultEndpointPathProvider}.
 *
 * @author Phillip Webb
 */
public class DefaultEndpointPathProviderTests {

	@Mock
	private EndpointDiscoverer<WebOperation> discoverer;

	@Before
	public void setup() {
		MockitoAnnotations.initMocks(this);
	}

	@Test
	public void getPathsShouldReturnAllPaths() throws Exception {
		DefaultEndpointPathProvider provider = createProvider("");
		assertThat(provider.getPaths()).containsOnly("/foo", "/bar");
	}

	@Test
	public void getPathsWhenHasContextPathShouldReturnAllPathsWithContext()
			throws Exception {
		DefaultEndpointPathProvider provider = createProvider("/application");
		assertThat(provider.getPaths()).containsOnly("/application/foo",
				"/application/bar");
	}

	@Test
	public void getPathWhenEndpointIdIsKnownShouldReturnPath() throws Exception {
		DefaultEndpointPathProvider provider = createProvider("");
		assertThat(provider.getPath("foo")).isEqualTo("/foo");
	}

	@Test
	public void getPathWhenEndpointIdIsUnknownShouldReturnNull() throws Exception {
		DefaultEndpointPathProvider provider = createProvider("");
		assertThat(provider.getPath("baz")).isNull();
	}

	@Test
	public void getPathWhenHasContextPathReturnPath() throws Exception {
		DefaultEndpointPathProvider provider = createProvider("/application");
		assertThat(provider.getPath("foo")).isEqualTo("/application/foo");
	}

	private DefaultEndpointPathProvider createProvider(String contextPath) {
		Collection<EndpointInfo<WebOperation>> endpoints = new ArrayList<>();
		endpoints.add(new EndpointInfo<>("foo", true, Collections.emptyList()));
		endpoints.add(new EndpointInfo<>("bar", true, Collections.emptyList()));
		given(this.discoverer.discoverEndpoints()).willReturn(endpoints);
		WebEndpointProperties webEndpointProperties = new WebEndpointProperties();
		webEndpointProperties.setBasePath(contextPath);
		return new DefaultEndpointPathProvider(this.discoverer, webEndpointProperties);
	}

}
