/*
 * Copyright 2012-2020 the original author or authors.
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

package org.springframework.boot.actuate.metrics.web.reactive.server;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import io.micrometer.core.instrument.Tag;
import org.junit.jupiter.api.Test;

import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link DefaultWebFluxTagsProvider}.
 *
 * @author Andy Wilkinson
 */
public class DefaultWebFluxTagsProviderTests {

	@Test
	void whenTagsAreProvidedThenDefaultTagsArePresent() {
		ServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/test"));
		Map<String, Tag> tags = asMap(new DefaultWebFluxTagsProvider().httpRequestTags(exchange, null));
		assertThat(tags).containsOnlyKeys("exception", "method", "outcome", "status", "uri");
	}

	@Test
	void givenSomeContributorsWhenTagsAreProvidedThenDefaultTagsAndContributedTagsArePresent() {
		ServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/test"));
		Map<String, Tag> tags = asMap(
				new DefaultWebFluxTagsProvider(Arrays.asList(new TestWebFluxTagsContributor("alpha"),
						new TestWebFluxTagsContributor("bravo", "charlie"))).httpRequestTags(exchange, null));
		assertThat(tags).containsOnlyKeys("exception", "method", "outcome", "status", "uri", "alpha", "bravo",
				"charlie");
	}

	private Map<String, Tag> asMap(Iterable<Tag> tags) {
		return StreamSupport.stream(tags.spliterator(), false)
				.collect(Collectors.toMap(Tag::getKey, Function.identity()));
	}

	private static final class TestWebFluxTagsContributor implements WebFluxTagsContributor {

		private final List<String> tagNames;

		private TestWebFluxTagsContributor(String... tagNames) {
			this.tagNames = Arrays.asList(tagNames);
		}

		@Override
		public Iterable<Tag> httpRequestTags(ServerWebExchange exchange, Throwable ex) {
			return this.tagNames.stream().map((name) -> Tag.of(name, "value")).collect(Collectors.toList());
		}

	}

}
