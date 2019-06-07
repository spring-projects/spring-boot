/*
 * Copyright 2012-2019 the original author or authors.
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

package org.springframework.boot.actuate.endpoint.web.servlet;

import io.micrometer.core.instrument.Tag;
import org.junit.Test;

import org.springframework.boot.actuate.metrics.web.servlet.WebMvcTags;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.util.pattern.PathPatternParser;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link WebMvcTags}.
 *
 * @author Andy Wilkinson
 * @author Brian Clozel
 */
public class WebMvcTagsTests {

	private final MockHttpServletRequest request = new MockHttpServletRequest();

	private final MockHttpServletResponse response = new MockHttpServletResponse();

	@Test
	public void uriTagIsDataRestsEffectiveRepositoryLookupPathWhenAvailable() {
		this.request.setAttribute(
				"org.springframework.data.rest.webmvc.RepositoryRestHandlerMapping.EFFECTIVE_REPOSITORY_RESOURCE_LOOKUP_PATH",
				new PathPatternParser().parse("/api/cities"));
		this.request.setAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE, "/api/{repository}");
		Tag tag = WebMvcTags.uri(this.request, this.response);
		assertThat(tag.getValue()).isEqualTo("/api/cities");
	}

	@Test
	public void uriTagValueIsBestMatchingPatternWhenAvailable() {
		this.request.setAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE, "/spring");
		this.response.setStatus(301);
		Tag tag = WebMvcTags.uri(this.request, this.response);
		assertThat(tag.getValue()).isEqualTo("/spring");
	}

	@Test
	public void uriTagValueIsRootWhenRequestHasNoPatternOrPathInfo() {
		assertThat(WebMvcTags.uri(this.request, null).getValue()).isEqualTo("root");
	}

	@Test
	public void uriTagValueIsRootWhenRequestHasNoPatternAndSlashPathInfo() {
		this.request.setPathInfo("/");
		assertThat(WebMvcTags.uri(this.request, null).getValue()).isEqualTo("root");
	}

	@Test
	public void uriTagValueIsUnknownWhenRequestHasNoPatternAndNonRootPathInfo() {
		this.request.setPathInfo("/example");
		assertThat(WebMvcTags.uri(this.request, null).getValue()).isEqualTo("UNKNOWN");
	}

	@Test
	public void uriTagValueIsRedirectionWhenResponseStatusIs3xx() {
		this.response.setStatus(301);
		Tag tag = WebMvcTags.uri(this.request, this.response);
		assertThat(tag.getValue()).isEqualTo("REDIRECTION");
	}

	@Test
	public void uriTagValueIsNotFoundWhenResponseStatusIs404() {
		this.response.setStatus(404);
		Tag tag = WebMvcTags.uri(this.request, this.response);
		assertThat(tag.getValue()).isEqualTo("NOT_FOUND");
	}

	@Test
	public void uriTagToleratesCustomResponseStatus() {
		this.response.setStatus(601);
		Tag tag = WebMvcTags.uri(this.request, this.response);
		assertThat(tag.getValue()).isEqualTo("root");
	}

	@Test
	public void uriTagIsUnknownWhenRequestIsNull() {
		Tag tag = WebMvcTags.uri(null, null);
		assertThat(tag.getValue()).isEqualTo("UNKNOWN");
	}

}
