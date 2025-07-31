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

package org.springframework.boot.http.client.autoconfigure;

import java.net.URI;
import java.util.Locale;

import org.junit.jupiter.api.Test;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.ApiVersionFormatter;
import org.springframework.web.client.ApiVersionInserter;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link PropertiesApiVersionInserter}.
 *
 * @author Phillip Webb
 */
class PropertiesApiVersionInserterTests {

	@Test
	void getWhenEmptyPropertiesArrayAndNoDeleteReturnsNull() {
		assertThat(PropertiesApiVersionInserter.get(null, null)).isNull();
	}

	@Test
	void getWhenNoPropertiesAndNoDelegateReturnsNull() {
		assertThat(PropertiesApiVersionInserter.get(null, null, new ApiversionProperties(), new ApiversionProperties()))
			.isNull();
	}

	@Test
	void getWhenNoPropertiesAndDelegateUsesDelegate() throws Exception {
		ApiVersionInserter inserter = PropertiesApiVersionInserter.get(ApiVersionInserter.useQueryParam("v"), null);
		URI uri = new URI("https://example.com");
		assertThat(inserter.insertVersion("123", uri)).hasToString("https://example.com?v=123");
	}

	@Test
	void getReturnsInserterThatAppliesProperties() throws Exception {
		ApiversionProperties properties1 = new ApiversionProperties();
		properties1.getInsert().setHeader("x-test");
		properties1.getInsert().setQueryParameter("v1");
		ApiversionProperties properties2 = new ApiversionProperties();
		properties2.getInsert().setQueryParameter("v2");
		properties2.getInsert().setPathSegment(1);
		properties2.getInsert().setMediaTypeParameter("mtp");
		ApiVersionInserter inserter = PropertiesApiVersionInserter.get(null, null, properties1, properties2);
		URI uri = new URI("https://example.com/foo/bar");
		assertThat(inserter.insertVersion("123", uri)).hasToString("https://example.com/foo/123/bar?v1=123&v2=123");
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		inserter.insertVersion("123", headers);
		assertThat(headers.get("x-test")).containsExactly("123");
		assertThat(headers.getContentType().getParameters()).containsEntry("mtp", "123");
	}

	@Test
	void getWhenHasDelegateReturnsInserterThatAppliesPropertiesAndDelegate() throws Exception {
		ApiVersionInserter delegate = ApiVersionInserter.useQueryParam("d");
		ApiversionProperties properties = new ApiversionProperties();
		properties.getInsert().setQueryParameter("v");
		ApiVersionInserter inserter = PropertiesApiVersionInserter.get(delegate, null, properties);
		assertThat(inserter.insertVersion("123", new URI("https://example.com")))
			.hasToString("https://example.com?d=123&v=123");
	}

	@Test
	void getWhenHasFormatterAppliesToProperties() throws Exception {
		ApiversionProperties properties1 = new ApiversionProperties();
		properties1.getInsert().setQueryParameter("v");
		ApiVersionFormatter formatter = (version) -> String.valueOf(version).toUpperCase(Locale.ROOT);
		ApiVersionInserter inserter = PropertiesApiVersionInserter.get(null, formatter, properties1);
		URI uri = new URI("https://example.com");
		assertThat(inserter.insertVersion("latest", uri)).hasToString("https://example.com?v=LATEST");
	}

}
