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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.PropertySource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link SystemEnvironmentPropertyMapper}.
 *
 * @author Phillip Webb
 * @author Madhura Bhave
 */
public class SystemEnvironmentPropertyMapperTests extends AbstractPropertyMapperTests {

	@Override
	protected PropertyMapper getMapper() {
		return SystemEnvironmentPropertyMapper.INSTANCE;
	}

	@Test
	public void mapFromStringShouldReturnBestGuess() throws Exception {
		assertThat(namesFromString("SERVER")).containsExactly("server");
		assertThat(namesFromString("SERVER_PORT")).containsExactly("server.port");
		assertThat(namesFromString("HOST_0")).containsExactly("host[0]");
		assertThat(namesFromString("HOST_0_1")).containsExactly("host[0][1]");
		assertThat(namesFromString("HOST_0_NAME")).containsExactly("host[0].name");
		assertThat(namesFromString("HOST_F00_NAME")).containsExactly("host.f00.name");
		assertThat(namesFromString("S-ERVER")).containsExactly("s-erver");
		assertThat(namesFromString("SERVERS__", "1,2,3")).containsExactly("servers[0]",
				"servers[1]", "servers[2]");
		assertThat(namesFromString("SERVERS_0__", "1,2,3"))
				.containsExactly("servers[0][0]", "servers[0][1]", "servers[0][2]");
	}

	@Test
	public void mapFromConfigurationShouldReturnBestGuess() throws Exception {
		assertThat(namesFromConfiguration("server")).containsExactly("SERVER");
		assertThat(namesFromConfiguration("server.port")).containsExactly("SERVER_PORT");
		assertThat(namesFromConfiguration("host[0]")).containsExactly("HOST_0");
		assertThat(namesFromConfiguration("host[0][1]")).containsExactly("HOST_0_1");
		assertThat(namesFromConfiguration("host[0].name")).containsExactly("HOST_0_NAME");
		assertThat(namesFromConfiguration("host.f00.name"))
				.containsExactly("HOST_F00_NAME");
		assertThat(namesFromConfiguration("foo.the-bar")).containsExactly("FOO_THEBAR");
	}

	@Test
	public void mapFromStringWhenListShortcutShouldExtractValues() throws Exception {
		Map<String, Object> source = new LinkedHashMap<>();
		source.put("SERVER__", "foo,bar,baz");
		PropertySource<?> propertySource = new MapPropertySource("test", source);
		List<PropertyMapping> mappings = getMapper().map(propertySource, "SERVER__");
		List<Object> result = new ArrayList<>();
		for (PropertyMapping mapping : mappings) {
			Object value = propertySource.getProperty(mapping.getPropertySourceName());
			value = mapping.getValueExtractor().apply(value);
			result.add(value);
		}
		assertThat(result).containsExactly("foo", "bar", "baz");
	}

	@Test
	public void mapFromConfigurationShouldIncludeShortcutAndExtractValues()
			throws Exception {
		Map<String, Object> source = new LinkedHashMap<>();
		source.put("SERVER__", "foo,bar,baz");
		PropertySource<?> propertySource = new MapPropertySource("test", source);
		List<PropertyMapping> mappings = getMapper().map(propertySource,
				ConfigurationPropertyName.of("server[1]"));
		List<Object> result = new ArrayList<>();
		for (PropertyMapping mapping : mappings) {
			Object value = propertySource.getProperty(mapping.getPropertySourceName());
			value = mapping.getValueExtractor().apply(value);
			if (value != null) {
				result.add(value);
			}
		}
		assertThat(result).containsExactly("bar");
	}

	@Test
	public void underscoreShouldNotMapToEmptyString() throws Exception {
		Map<String, Object> source = new LinkedHashMap<>();
		PropertySource<?> propertySource = new MapPropertySource("test", source);
		List<PropertyMapping> mappings = getMapper().map(propertySource, "_");
		boolean applicable = false;
		for (PropertyMapping mapping : mappings) {
			applicable = mapping.isApplicable(ConfigurationPropertyName.of(""));
		}
		assertThat(applicable).isFalse();
	}

	@Test
	public void underscoreWithWhitespaceShouldNotMapToEmptyString() throws Exception {
		Map<String, Object> source = new LinkedHashMap<>();
		PropertySource<?> propertySource = new MapPropertySource("test", source);
		List<PropertyMapping> mappings = getMapper().map(propertySource, "  _");
		boolean applicable = false;
		for (PropertyMapping mapping : mappings) {
			applicable = mapping.isApplicable(ConfigurationPropertyName.of(""));
		}
		assertThat(applicable).isFalse();
	}

}
