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

package org.springframework.boot.actuate.endpoint.web;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;

import org.assertj.core.api.Condition;
import org.junit.Test;

import org.springframework.boot.actuate.endpoint.DefaultEnablement;
import org.springframework.boot.actuate.endpoint.EndpointInfo;
import org.springframework.boot.actuate.endpoint.OperationType;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link EndpointLinksResolver}.
 *
 * @author Andy Wilkinson
 */
public class EndpointLinksResolverTests {

	private final EndpointLinksResolver linksResolver = new EndpointLinksResolver();

	@Test
	public void linkResolutionWithTrailingSlashStripsSlashOnSelfLink() {
		Map<String, Link> links = this.linksResolver.resolveLinks(Collections.emptyList(),
				"https://api.example.com/application/");
		assertThat(links).hasSize(1);
		assertThat(links).hasEntrySatisfying("self",
				linkWithHref("https://api.example.com/application"));
	}

	@Test
	public void linkResolutionWithoutTrailingSlash() {
		Map<String, Link> links = this.linksResolver.resolveLinks(Collections.emptyList(),
				"https://api.example.com/application");
		assertThat(links).hasSize(1);
		assertThat(links).hasEntrySatisfying("self",
				linkWithHref("https://api.example.com/application"));
	}

	@Test
	public void resolvedLinksContainsALinkForEachEndpointOperation() {
		Map<String, Link> links = this.linksResolver
				.resolveLinks(
						Arrays.asList(new EndpointInfo<>("alpha",
								DefaultEnablement.ENABLED,
								Arrays.asList(operationWithPath("/alpha", "alpha"),
										operationWithPath("/alpha/{name}",
												"alpha-name")))),
				"https://api.example.com/application");
		assertThat(links).hasSize(3);
		assertThat(links).hasEntrySatisfying("self",
				linkWithHref("https://api.example.com/application"));
		assertThat(links).hasEntrySatisfying("alpha",
				linkWithHref("https://api.example.com/application/alpha"));
		assertThat(links).hasEntrySatisfying("alpha-name",
				linkWithHref("https://api.example.com/application/alpha/{name}"));
	}

	private WebEndpointOperation operationWithPath(String path, String id) {
		return new WebEndpointOperation(OperationType.READ, null, false,
				new OperationRequestPredicate(path, WebEndpointHttpMethod.GET,
						Collections.emptyList(), Collections.emptyList()),
				id);
	}

	private Condition<Link> linkWithHref(String href) {
		return new Condition<>((link) -> href.equals(link.getHref()),
				"Link with href '%s'", href);
	}

}
