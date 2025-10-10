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

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Tests for {@link MapConfigurationPropertySource}.
 *
 * @author Phillip Webb
 * @author Madhura Bhave
 */
class MapConfigurationPropertySourceTests {

	@Test
	@SuppressWarnings("NullAway") // Test null check
	void createWhenMapIsNullShouldThrowException() {
		assertThatIllegalArgumentException().isThrownBy(() -> new MapConfigurationPropertySource(null))
			.withMessageContaining("'map' must not be null");
	}

	@Test
	void createWhenMapHasEntriesShouldAdaptMap() {
		Map<Object, Object> map = new LinkedHashMap<>();
		map.put("foo.BAR", "spring");
		map.put(ConfigurationPropertyName.of("foo.baz"), "boot");
		MapConfigurationPropertySource source = new MapConfigurationPropertySource(map);
		assertThat(getValue(source, "foo.bar")).isEqualTo("spring");
		assertThat(getValue(source, "foo.baz")).isEqualTo("boot");
	}

	@Test
	@SuppressWarnings("NullAway") // Test null check
	void putAllWhenMapIsNullShouldThrowException() {
		MapConfigurationPropertySource source = new MapConfigurationPropertySource();
		assertThatIllegalArgumentException().isThrownBy(() -> source.putAll(null))
			.withMessageContaining("'map' must not be null");
	}

	@Test
	void putAllShouldPutEntries() {
		Map<Object, Object> map = new LinkedHashMap<>();
		map.put("foo.BAR", "spring");
		map.put("foo.baz", "boot");
		MapConfigurationPropertySource source = new MapConfigurationPropertySource();
		source.putAll(map);
		assertThat(getValue(source, "foo.bar")).isEqualTo("spring");
		assertThat(getValue(source, "foo.baz")).isEqualTo("boot");
	}

	@Test
	void putShouldPutEntry() {
		MapConfigurationPropertySource source = new MapConfigurationPropertySource();
		source.put("foo.bar", "baz");
		assertThat(getValue(source, "foo.bar")).isEqualTo("baz");
	}

	@Test
	void getConfigurationPropertyShouldGetFromMemory() {
		MapConfigurationPropertySource source = new MapConfigurationPropertySource();
		source.put("foo.bar", "baz");
		assertThat(getValue(source, "foo.bar")).isEqualTo("baz");
		source.put("foo.bar", "big");
		assertThat(getValue(source, "foo.bar")).isEqualTo("big");
	}

	@Test
	void iteratorShouldGetFromMemory() {
		MapConfigurationPropertySource source = new MapConfigurationPropertySource();
		source.put("foo.BAR", "spring");
		source.put("foo.baz", "boot");
		assertThat(source.iterator()).toIterable()
			.containsExactly(ConfigurationPropertyName.of("foo.bar"), ConfigurationPropertyName.of("foo.baz"));
	}

	@Test
	void streamShouldGetFromMemory() {
		MapConfigurationPropertySource source = new MapConfigurationPropertySource();
		source.put("foo.BAR", "spring");
		source.put("foo.baz", "boot");
		assertThat(source.stream()).containsExactly(ConfigurationPropertyName.of("foo.bar"),
				ConfigurationPropertyName.of("foo.baz"));

	}

	private @Nullable Object getValue(ConfigurationPropertySource source, String name) {
		ConfigurationProperty property = source.getConfigurationProperty(ConfigurationPropertyName.of(name));
		return (property != null) ? property.getValue() : null;
	}

}
