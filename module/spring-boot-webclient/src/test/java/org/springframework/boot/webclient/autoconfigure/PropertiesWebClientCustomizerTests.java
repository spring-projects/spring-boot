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

package org.springframework.boot.webclient.autoconfigure;

import java.net.URI;
import java.util.List;
import java.util.Locale;

import org.junit.jupiter.api.Test;

import org.springframework.http.HttpHeaders;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.ApiVersionFormatter;
import org.springframework.web.client.ApiVersionInserter;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriBuilderFactory;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link PropertiesWebClientCustomizer}.
 *
 * @author Phillip Webb
 */
class PropertiesWebClientCustomizerTests {

	@Test
	void customizeAppliesPropertiesInOrder() throws Exception {
		ApiVersionInserter delegateApiVersionInserter = ApiVersionInserter.useQueryParam("v");
		ApiVersionFormatter apiVersionFormatter = (version) -> String.valueOf(version).toUpperCase(Locale.ROOT);
		TestWebClientProperties properties1 = new TestWebClientProperties();
		properties1.setBaseUrl("https://example.com/b1");
		properties1.getDefaultHeader().put("x-h1", List.of("v1"));
		properties1.getApiversion().setDefaultVersion("dv1");
		properties1.getApiversion().getInsert().setQueryParameter("p1");
		TestWebClientProperties properties2 = new TestWebClientProperties();
		properties2.setBaseUrl("https://example.com/b2");
		properties1.getDefaultHeader().put("x-h2", List.of("v2"));
		properties2.getApiversion().setDefaultVersion("dv2");
		PropertiesWebClientCustomizer customizer = new PropertiesWebClientCustomizer(delegateApiVersionInserter,
				apiVersionFormatter, properties1, properties2);
		WebClient.Builder builder = WebClient.builder();
		customizer.customize(builder);
		WebClient client = builder.build();
		assertThat(client).extracting("defaultApiVersion").isEqualTo("dv1");
		UriBuilderFactory uriBuilderFactory = (UriBuilderFactory) ReflectionTestUtils.getField(client,
				"uriBuilderFactory");
		assertThat(uriBuilderFactory.builder().build()).hasToString("https://example.com/b1");
		HttpHeaders defaultHeaders = (HttpHeaders) ReflectionTestUtils.getField(client, "defaultHeaders");
		assertThat(defaultHeaders.get("x-h1")).containsExactly("v1");
		assertThat(defaultHeaders.get("x-h2")).containsExactly("v2");
		ApiVersionInserter apiVersionInserter = (ApiVersionInserter) ReflectionTestUtils.getField(client,
				"apiVersionInserter");
		assertThat(apiVersionInserter.insertVersion("v123", new URI("https://example.com")))
			.hasToString("https://example.com?v=v123&p1=V123");
	}

	static class TestWebClientProperties extends AbstractWebClientProperties {

	}

}
