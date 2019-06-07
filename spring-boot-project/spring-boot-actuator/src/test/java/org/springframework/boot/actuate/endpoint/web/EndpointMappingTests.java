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

package org.springframework.boot.actuate.endpoint.web;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link EndpointMapping}.
 *
 * @author Andy Wilkinson
 */
public class EndpointMappingTests {

	@Test
	public void normalizationTurnsASlashIntoAnEmptyString() {
		assertThat(new EndpointMapping("/").getPath()).isEqualTo("");
	}

	@Test
	public void normalizationLeavesAnEmptyStringAsIs() {
		assertThat(new EndpointMapping("").getPath()).isEqualTo("");
	}

	@Test
	public void normalizationRemovesATrailingSlash() {
		assertThat(new EndpointMapping("/test/").getPath()).isEqualTo("/test");
	}

	@Test
	public void normalizationAddsALeadingSlash() {
		assertThat(new EndpointMapping("test").getPath()).isEqualTo("/test");
	}

	@Test
	public void normalizationAddsALeadingSlashAndRemovesATrailingSlash() {
		assertThat(new EndpointMapping("test/").getPath()).isEqualTo("/test");
	}

	@Test
	public void normalizationLeavesAPathWithALeadingSlashAndNoTrailingSlashAsIs() {
		assertThat(new EndpointMapping("/test").getPath()).isEqualTo("/test");
	}

	@Test
	public void subPathForAnEmptyStringReturnsBasePath() {
		assertThat(new EndpointMapping("/test").createSubPath("")).isEqualTo("/test");
	}

	@Test
	public void subPathWithALeadingSlashIsSeparatedFromBasePathBySingleSlash() {
		assertThat(new EndpointMapping("/test").createSubPath("/one")).isEqualTo("/test/one");
	}

	@Test
	public void subPathWithoutALeadingSlashIsSeparatedFromBasePathBySingleSlash() {
		assertThat(new EndpointMapping("/test").createSubPath("one")).isEqualTo("/test/one");
	}

	@Test
	public void trailingSlashIsRemovedFromASubPath() {
		assertThat(new EndpointMapping("/test").createSubPath("one/")).isEqualTo("/test/one");
	}

}
