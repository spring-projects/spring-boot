/*
 * Copyright 2012-2021 the original author or authors.
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

package org.springframework.boot.context.properties.source;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link PrefixedIterableConfigurationPropertySource}.
 *
 * @author Madhura Bhave
 */
class PrefixedIterableConfigurationPropertySourceTests {

	@Test
	void streamShouldConsiderPrefix() {
		MockConfigurationPropertySource source = new MockConfigurationPropertySource();
		source.put("my.foo.bar", "bing");
		source.put("my.foo.baz", "biff");
		source.put("hello.bing", "blah");
		IterableConfigurationPropertySource prefixed = source.withPrefix("my");
		assertThat(prefixed.stream()).containsExactly(ConfigurationPropertyName.of("foo.bar"),
				ConfigurationPropertyName.of("foo.baz"), ConfigurationPropertyName.of("hello.bing"));
	}

	@Test
	void emptyPrefixShouldReturnOriginalStream() {
		MockConfigurationPropertySource source = new MockConfigurationPropertySource();
		source.put("my.foo.bar", "bing");
		source.put("my.foo.baz", "biff");
		source.put("hello.bing", "blah");
		IterableConfigurationPropertySource prefixed = source.withPrefix("");
		assertThat(prefixed.stream()).containsExactly(ConfigurationPropertyName.of("my.foo.bar"),
				ConfigurationPropertyName.of("my.foo.baz"), ConfigurationPropertyName.of("hello.bing"));
	}

}
