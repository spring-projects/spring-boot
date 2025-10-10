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

package org.springframework.boot.context.properties.bind;

import java.util.HashMap;
import java.util.Map;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

import org.springframework.core.env.Environment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertySources;
import org.springframework.util.PropertyPlaceholderHelper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Tests for {@link PropertySourcesPlaceholdersResolver}.
 *
 * @author Phillip Webb
 * @author Madhura Bhave
 */
class PropertySourcesPlaceholdersResolverTests {

	private @Nullable PropertySourcesPlaceholdersResolver resolver;

	@Test
	@SuppressWarnings("NullAway") // Test null check
	void placeholderResolverIfEnvironmentNullShouldThrowException() {
		assertThatIllegalArgumentException()
			.isThrownBy(() -> new PropertySourcesPlaceholdersResolver((Environment) null))
			.withMessageContaining("'environment' must not be null");
	}

	@Test
	void resolveIfPlaceholderPresentResolvesProperty() {
		MutablePropertySources sources = getPropertySources();
		this.resolver = new PropertySourcesPlaceholdersResolver(sources);
		Object resolved = this.resolver.resolvePlaceholders("${FOO}");
		assertThat(resolved).isEqualTo("hello world");
	}

	@Test
	void resolveIfPlaceholderAbsentUsesDefault() {
		this.resolver = new PropertySourcesPlaceholdersResolver((PropertySources) null);
		Object resolved = this.resolver.resolvePlaceholders("${FOO:bar}");
		assertThat(resolved).isEqualTo("bar");
	}

	@Test
	void resolveIfPlaceholderAbsentAndNoDefaultUsesPlaceholder() {
		this.resolver = new PropertySourcesPlaceholdersResolver((PropertySources) null);
		Object resolved = this.resolver.resolvePlaceholders("${FOO}");
		assertThat(resolved).isEqualTo("${FOO}");
	}

	@Test
	void resolveIfHelperPresentShouldUseIt() {
		MutablePropertySources sources = getPropertySources();
		TestPropertyPlaceholderHelper helper = new TestPropertyPlaceholderHelper("$<", ">");
		this.resolver = new PropertySourcesPlaceholdersResolver(sources, helper);
		Object resolved = this.resolver.resolvePlaceholders("$<FOO>");
		assertThat(resolved).isEqualTo("hello world");
	}

	private MutablePropertySources getPropertySources() {
		MutablePropertySources sources = new MutablePropertySources();
		Map<String, Object> source = new HashMap<>();
		source.put("FOO", "hello world");
		sources.addFirst(new MapPropertySource("test", source));
		return sources;
	}

	static class TestPropertyPlaceholderHelper extends PropertyPlaceholderHelper {

		TestPropertyPlaceholderHelper(String placeholderPrefix, String placeholderSuffix) {
			super(placeholderPrefix, placeholderSuffix);
		}

	}

}
