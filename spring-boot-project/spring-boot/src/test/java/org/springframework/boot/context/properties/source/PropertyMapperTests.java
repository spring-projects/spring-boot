/*
 * Copyright 2012-2023 the original author or authors.
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

import java.util.Collections;
import java.util.Map;

import org.junit.jupiter.api.Test;

import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.env.SystemEnvironmentPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link PropertyMappers}.
 *
 * @author Wang Zhiyang
 */
public class PropertyMapperTests {

	@Test
	void mapperSystemEnvironment() {
		SystemEnvironmentPropertySource systemEnvironmentPropertySource = new SystemEnvironmentPropertySource(
				StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME,
				Collections.singletonMap("TEST_MAP_FOO_BAR", "baz"));
		String candidate = PropertyMappers.map(systemEnvironmentPropertySource,
				ConfigurationPropertyName.of("test.map.foo.bar"));
		assertThat(candidate).isEqualTo("TEST_MAP_FOO_BAR");
	}

	@Test
	void testMapPropertySource() {
		Map<String, Object> source = Collections.singletonMap("custom.map-with-replacement.key", "1");
		MapPropertySource mapPropertySource = new MapPropertySource("map", source);
		String candidate = PropertyMappers.map(mapPropertySource,
				ConfigurationPropertyName.of("custom.map-with-replacement.key"));
		assertThat(candidate).isEqualTo("custom.map-with-replacement.key");
	}

}
