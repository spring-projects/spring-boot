/*
 * Copyright 2012-present the original author or authors.
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

package org.springframework.boot.test.http.client;

import java.net.URI;
import java.util.Map;

import org.junit.jupiter.api.Test;

import org.springframework.boot.test.http.server.BaseUrl;
import org.springframework.web.util.UriBuilderFactory;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link BaseUrlUriBuilderFactory}.
 *
 * @author Stephane Nicoll
 */
class BaseUrlUriBuilderFactoryTests {

	@Test
	void uriWithRootSlashAddsBaseUrl() {
		UriBuilderFactory factory = BaseUrlUriBuilderFactory.get(BaseUrl.of("https://example.com"));
		assertThat(factory.uriString("/").build()).isEqualTo(URI.create("https://example.com/"));
	}

	@Test
	void uriWithEmptyAddsBaseUrl() {
		UriBuilderFactory factory = BaseUrlUriBuilderFactory.get(BaseUrl.of("https://example.com"));
		assertThat(factory.uriString("").build()).isEqualTo(URI.create("https://example.com"));
	}

	@Test
	void uriWithMapVariablesAddsBaseUrl() {
		UriBuilderFactory factory = BaseUrlUriBuilderFactory.get(BaseUrl.of("https://example.com"));
		assertThat(factory.expand("/test/{name}", Map.of("name", "value")))
			.isEqualTo(URI.create("https://example.com/test/value"));
	}

	@Test
	void uriWithVariablesAddsBaseUrl() {
		UriBuilderFactory factory = BaseUrlUriBuilderFactory.get(BaseUrl.of("https://example.com"));
		assertThat(factory.expand("/test/{name}", "value")).isEqualTo(URI.create("https://example.com/test/value"));
	}

	@Test
	void uriWithHostDoesNotExpandBaseUrl() {
		UriBuilderFactory factory = BaseUrlUriBuilderFactory.get(BaseUrl.of("https://example.com"));
		assertThat(factory.uriString("https://sub.example.com").build())
			.isEqualTo(URI.create("https://sub.example.com"));
	}

}
