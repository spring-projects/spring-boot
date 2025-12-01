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

package org.springframework.boot.restclient;

import java.net.URI;
import java.net.URISyntaxException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.web.util.UriBuilder;
import org.springframework.web.util.UriBuilderFactory;
import org.springframework.web.util.UriTemplateHandler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link RootUriBuilderFactory}.
 *
 * @author Scott Frederick
 * @author Hyunwoo Gu
 */
class RootUriBuilderFactoryTests {

	private String rootUri;

	private UriBuilderFactory builderFactory;

	@BeforeEach
	void setUp() {
		this.rootUri = "https://example.com";
		this.builderFactory = new RootUriBuilderFactory(this.rootUri, mock(UriTemplateHandler.class));
	}

	@Test
	void uriStringPrefixesRoot() throws URISyntaxException {
		UriBuilder builder = this.builderFactory.uriString("/hello");
		assertThat(builder.build()).isEqualTo(new URI("https://example.com/hello"));
	}

	@Test
	void uriStringWhenEmptyShouldReturnRoot() throws URISyntaxException {
		UriBuilder builder = this.builderFactory.uriString("");

		URI builtUri = builder.build();
		assertThat(builtUri).isEqualTo(new URI(this.rootUri));
	}

}
