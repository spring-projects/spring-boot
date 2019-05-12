/*
 * Copyright 2012-2019 the original author or authors.
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

import org.junit.Test;

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
	public void mapFromStringShouldReturnBestGuess() {
		assertThat(namesFromString("SERVER")).toIterable().containsExactly("server");
		assertThat(namesFromString("SERVER_PORT")).toIterable()
				.containsExactly("server.port");
		assertThat(namesFromString("HOST_0")).toIterable().containsExactly("host[0]");
		assertThat(namesFromString("HOST_0_1")).toIterable()
				.containsExactly("host[0][1]");
		assertThat(namesFromString("HOST_0_NAME")).toIterable()
				.containsExactly("host[0].name");
		assertThat(namesFromString("HOST_F00_NAME")).toIterable()
				.containsExactly("host.f00.name");
		assertThat(namesFromString("S-ERVER")).toIterable().containsExactly("s-erver");
	}

	@Test
	public void mapFromConfigurationShouldReturnBestGuess() {
		assertThat(namesFromConfiguration("server")).toIterable()
				.containsExactly("SERVER");
		assertThat(namesFromConfiguration("server.port")).toIterable()
				.containsExactly("SERVER_PORT");
		assertThat(namesFromConfiguration("host[0]")).toIterable()
				.containsExactly("HOST_0");
		assertThat(namesFromConfiguration("host[0][1]")).toIterable()
				.containsExactly("HOST_0_1");
		assertThat(namesFromConfiguration("host[0].name")).toIterable()
				.containsExactly("HOST_0_NAME");
		assertThat(namesFromConfiguration("host.f00.name")).toIterable()
				.containsExactly("HOST_F00_NAME");
		assertThat(namesFromConfiguration("foo.the-bar")).toIterable()
				.containsExactly("FOO_THEBAR", "FOO_THE_BAR");
	}

	@Test
	public void underscoreShouldNotMapToEmptyString() {
		PropertyMapping[] mappings = getMapper().map("_");
		boolean applicable = false;
		for (PropertyMapping mapping : mappings) {
			applicable = mapping.isApplicable(ConfigurationPropertyName.of(""));
		}
		assertThat(applicable).isFalse();
	}

	@Test
	public void underscoreWithWhitespaceShouldNotMapToEmptyString() {
		PropertyMapping[] mappings = getMapper().map("  _");
		boolean applicable = false;
		for (PropertyMapping mapping : mappings) {
			applicable = mapping.isApplicable(ConfigurationPropertyName.of(""));
		}
		assertThat(applicable).isFalse();
	}

}
