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

package org.springframework.boot.context.properties.source;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link DefaultPropertyMapper}.
 *
 * @author Phillip Webb
 * @author Madhura Bhave
 */
class DefaultPropertyMapperTests extends AbstractPropertyMapperTests {

	@Override
	protected PropertyMapper getMapper() {
		return DefaultPropertyMapper.INSTANCE;
	}

	@Test
	void mapFromStringShouldReturnBestGuess() {
		assertThat(mapPropertySourceName("server")).isEqualTo("server");
		assertThat(mapPropertySourceName("server.port")).isEqualTo("server.port");
		assertThat(mapPropertySourceName("host[0]")).isEqualTo("host[0]");
		assertThat(mapPropertySourceName("host[0][1]")).isEqualTo("host[0][1]");
		assertThat(mapPropertySourceName("host[0].name")).isEqualTo("host[0].name");
		assertThat(mapPropertySourceName("host.f00.name")).isEqualTo("host.f00.name");
		assertThat(mapPropertySourceName("my.host-name")).isEqualTo("my.host-name");
		assertThat(mapPropertySourceName("my.hostName")).isEqualTo("my.hostname");
		assertThat(mapPropertySourceName("my.HOST_NAME")).isEqualTo("my.hostname");
		assertThat(mapPropertySourceName("s[!@#$%^&*()=+]e-rVeR")).isEqualTo("s[!@#$%^&*()=+].e-rver");
		assertThat(mapPropertySourceName("host[FOO].name")).isEqualTo("host[FOO].name");
	}

	@Test
	void mapFromConfigurationShouldReturnBestGuess() {
		assertThat(mapConfigurationPropertyName("server")).containsExactly("server");
		assertThat(mapConfigurationPropertyName("server.port")).containsExactly("server.port");
		assertThat(mapConfigurationPropertyName("host[0]")).containsExactly("host[0]");
		assertThat(mapConfigurationPropertyName("host[0][1]")).containsExactly("host[0][1]");
		assertThat(mapConfigurationPropertyName("host[0].name")).containsExactly("host[0].name");
		assertThat(mapConfigurationPropertyName("host.f00.name")).containsExactly("host.f00.name");
		assertThat(mapConfigurationPropertyName("my.host-name")).containsExactly("my.host-name");
		assertThat(mapConfigurationPropertyName("host[FOO].name")).containsExactly("host[FOO].name");
	}

}
