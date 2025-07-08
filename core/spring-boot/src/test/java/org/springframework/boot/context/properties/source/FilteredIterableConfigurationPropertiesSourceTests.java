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

package org.springframework.boot.context.properties.source;

import java.util.LinkedHashMap;
import java.util.Map;

import org.assertj.core.extractor.Extractors;
import org.junit.jupiter.api.Test;

import org.springframework.boot.env.OriginTrackedMapPropertySource;
import org.springframework.core.env.PropertySource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test for {@link FilteredIterableConfigurationPropertiesSource}.
 *
 * @author Phillip Webb
 * @author Madhura Bhave
 */
class FilteredIterableConfigurationPropertiesSourceTests extends FilteredConfigurationPropertiesSourceTests {

	@Test
	void iteratorFiltersNames() {
		MockConfigurationPropertySource source = (MockConfigurationPropertySource) createTestSource();
		IterableConfigurationPropertySource filtered = source.filter(this::noBrackets);
		assertThat(filtered.iterator()).toIterable()
			.extracting(ConfigurationPropertyName::toString)
			.containsExactly("a", "b", "c");
	}

	@Test
	void containsDescendantOfUsesContents() {
		MockConfigurationPropertySource source = new MockConfigurationPropertySource();
		source.put("foo.bar.baz", "1");
		source.put("foo.bar[0]", "1");
		source.put("faf.bar[0]", "1");
		IterableConfigurationPropertySource filtered = source.filter(this::noBrackets);
		assertThat(filtered.containsDescendantOf(ConfigurationPropertyName.of("foo")))
			.isEqualTo(ConfigurationPropertyState.PRESENT);
		assertThat(filtered.containsDescendantOf(ConfigurationPropertyName.of("faf")))
			.isEqualTo(ConfigurationPropertyState.ABSENT);
	}

	@Test
	void iteratorWhenSpringPropertySourceFiltersNames() {
		IterableConfigurationPropertySource testSource = (IterableConfigurationPropertySource) createTestSource();
		Map<String, Object> map = new LinkedHashMap<>();
		for (ConfigurationPropertyName name : testSource) {
			map.put(name.toString(), testSource.getConfigurationProperty(name).getValue());
		}
		PropertySource<?> propertySource = new OriginTrackedMapPropertySource("test", map, true);
		SpringConfigurationPropertySource source = SpringConfigurationPropertySource.from(propertySource);
		IterableConfigurationPropertySource filtered = (IterableConfigurationPropertySource) source
			.filter(this::noBrackets);
		assertThat(Extractors.byName("filteredNames").apply(filtered)).isNotNull();
		assertThat(filtered.iterator()).toIterable()
			.extracting(ConfigurationPropertyName::toString)
			.containsExactly("a", "b", "c");
	}

	@Test
	void iteratorWhenSpringPropertySourceAndAnotherFilterFiltersNames() {
		IterableConfigurationPropertySource testSource = (IterableConfigurationPropertySource) createTestSource();
		Map<String, Object> map = new LinkedHashMap<>();
		for (ConfigurationPropertyName name : testSource) {
			map.put(name.toString(), testSource.getConfigurationProperty(name).getValue());
		}
		PropertySource<?> propertySource = new OriginTrackedMapPropertySource("test", map, true);
		SpringConfigurationPropertySource source = SpringConfigurationPropertySource.from(propertySource);
		IterableConfigurationPropertySource filtered = (IterableConfigurationPropertySource) source
			.filter(this::noBrackets);
		IterableConfigurationPropertySource secondFiltered = filtered.filter((name) -> !name.toString().contains("c"));
		assertThat(Extractors.byName("filteredNames").apply(filtered)).isNotNull();
		assertThat(secondFiltered.iterator()).toIterable()
			.extracting(ConfigurationPropertyName::toString)
			.containsExactly("a", "b");
	}

	@Test
	void containsDescendantOfWhenSpringPropertySourceUsesContents() {
		Map<String, Object> map = new LinkedHashMap<>();
		map.put("foo.bar.baz", "1");
		map.put("foo.bar[0]", "1");
		map.put("faf.bar[0]", "1");
		PropertySource<?> propertySource = new OriginTrackedMapPropertySource("test", map, true);
		SpringConfigurationPropertySource source = SpringConfigurationPropertySource.from(propertySource);
		IterableConfigurationPropertySource filtered = (IterableConfigurationPropertySource) source
			.filter(this::noBrackets);
		assertThat(Extractors.byName("filteredNames").apply(filtered)).isNotNull();
		assertThat(filtered.containsDescendantOf(ConfigurationPropertyName.of("foo")))
			.isEqualTo(ConfigurationPropertyState.PRESENT);
		assertThat(filtered.containsDescendantOf(ConfigurationPropertyName.of("faf")))
			.isEqualTo(ConfigurationPropertyState.ABSENT);
	}

	@Override
	protected ConfigurationPropertySource convertSource(MockConfigurationPropertySource source) {
		return source;
	}

	private boolean noBrackets(ConfigurationPropertyName name) {
		return !name.toString().contains("[");
	}

}
