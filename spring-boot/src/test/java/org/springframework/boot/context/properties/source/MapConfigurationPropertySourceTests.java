/*
 * Copyright 2012-2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
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

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link MapConfigurationPropertySource}.
 *
 * @author Phillip Webb
 * @author Madhura Bhave
 */
public class MapConfigurationPropertySourceTests {

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	@Test
	public void createWhenMapIsNullShouldThrowException() throws Exception {
		this.thrown.expect(IllegalArgumentException.class);
		this.thrown.expectMessage("Map must not be null");
		new MapConfigurationPropertySource(null);
	}

	@Test
	public void createWhenMapHasEntriesShouldAdaptMap() throws Exception {
		Map<Object, Object> map = new LinkedHashMap<>();
		map.put("foo.BAR", "spring");
		map.put(ConfigurationPropertyName.of("foo.baz"), "boot");
		MapConfigurationPropertySource source = new MapConfigurationPropertySource(map);
		assertThat(getValue(source, "foo.bar")).isEqualTo("spring");
		assertThat(getValue(source, "foo.baz")).isEqualTo("boot");
	}

	@Test
	public void putAllWhenMapIsNullShouldThrowException() throws Exception {
		this.thrown.expect(IllegalArgumentException.class);
		this.thrown.expectMessage("Map must not be null");
		MapConfigurationPropertySource source = new MapConfigurationPropertySource();
		source.putAll(null);
	}

	@Test
	public void putAllShouldPutEntries() throws Exception {
		Map<Object, Object> map = new LinkedHashMap<>();
		map.put("foo.BAR", "spring");
		map.put("foo.baz", "boot");
		MapConfigurationPropertySource source = new MapConfigurationPropertySource();
		source.putAll(map);
		assertThat(getValue(source, "foo.bar")).isEqualTo("spring");
		assertThat(getValue(source, "foo.baz")).isEqualTo("boot");
	}

	@Test
	public void putShouldPutEntry() throws Exception {
		MapConfigurationPropertySource source = new MapConfigurationPropertySource();
		source.put("foo.bar", "baz");
		assertThat(getValue(source, "foo.bar")).isEqualTo("baz");
	}

	@Test
	public void getConfigurationPropertyShouldGetFromMemory() throws Exception {
		MapConfigurationPropertySource source = new MapConfigurationPropertySource();
		source.put("foo.bar", "baz");
		assertThat(getValue(source, "foo.bar")).isEqualTo("baz");
		source.put("foo.bar", "big");
		assertThat(getValue(source, "foo.bar")).isEqualTo("big");
	}

	@Test
	public void iteratorShouldGetFromMemory() throws Exception {
		MapConfigurationPropertySource source = new MapConfigurationPropertySource();
		source.put("foo.BAR", "spring");
		source.put("foo.baz", "boot");
		assertThat(source.iterator()).containsExactly(
				ConfigurationPropertyName.of("foo.bar"),
				ConfigurationPropertyName.of("foo.baz"));
	}

	@Test
	public void streamShouldGetFromMemory() throws Exception {
		MapConfigurationPropertySource source = new MapConfigurationPropertySource();
		source.put("foo.BAR", "spring");
		source.put("foo.baz", "boot");
		assertThat(source.stream()).containsExactly(
				ConfigurationPropertyName.of("foo.bar"),
				ConfigurationPropertyName.of("foo.baz"));

	}

	private Object getValue(ConfigurationPropertySource source, String name) {
		ConfigurationProperty property = source
				.getConfigurationProperty(ConfigurationPropertyName.of(name));
		return (property == null ? null : property.getValue());
	};

}
