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

import java.util.function.BiPredicate;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link SystemEnvironmentPropertyMapper}.
 *
 * @author Phillip Webb
 * @author Madhura Bhave
 */
class SystemEnvironmentPropertyMapperTests extends AbstractPropertyMapperTests {

	@Override
	protected PropertyMapper getMapper() {
		return SystemEnvironmentPropertyMapper.INSTANCE;
	}

	@Test
	void mapFromStringShouldReturnBestGuess() {
		assertThat(mapPropertySourceName("SERVER")).isEqualTo("server");
		assertThat(mapPropertySourceName("SERVER_PORT")).isEqualTo("server.port");
		assertThat(mapPropertySourceName("HOST_0")).isEqualTo("host[0]");
		assertThat(mapPropertySourceName("HOST_0_1")).isEqualTo("host[0][1]");
		assertThat(mapPropertySourceName("HOST_0_NAME")).isEqualTo("host[0].name");
		assertThat(mapPropertySourceName("HOST_F00_NAME")).isEqualTo("host.f00.name");
		assertThat(mapPropertySourceName("S-ERVER")).isEqualTo("s-erver");
	}

	@Test
	void mapFromConfigurationShouldReturnBestGuess() {
		assertThat(mapConfigurationPropertyName("server")).containsExactly("SERVER");
		assertThat(mapConfigurationPropertyName("server.port")).containsExactly("SERVER_PORT");
		assertThat(mapConfigurationPropertyName("host[0]")).containsExactly("HOST_0");
		assertThat(mapConfigurationPropertyName("host[0][1]")).containsExactly("HOST_0_1");
		assertThat(mapConfigurationPropertyName("host[0].name")).containsExactly("HOST_0_NAME");
		assertThat(mapConfigurationPropertyName("host.f00.name")).containsExactly("HOST_F00_NAME");
		assertThat(mapConfigurationPropertyName("foo.the-bar")).containsExactly("FOO_THEBAR", "FOO_THE_BAR");
	}

	@Test
	void underscoreShouldMapToEmptyString() {
		ConfigurationPropertyName mapped = getMapper().map("_");
		assertThat(mapped.isEmpty()).isTrue();
	}

	@Test
	void underscoreWithWhitespaceShouldMapToEmptyString() {
		ConfigurationPropertyName mapped = getMapper().map(" _");
		assertThat(mapped.isEmpty()).isTrue();
	}

	@Test
	void isAncestorOfConsidersLegacyNames() {
		ConfigurationPropertyName name = ConfigurationPropertyName.of("my.spring-boot");
		BiPredicate<ConfigurationPropertyName, ConfigurationPropertyName> check = getMapper().getAncestorOfCheck();
		assertThat(check.test(name, ConfigurationPropertyName.adapt("MY_SPRING_BOOT_PROPERTY", '_'))).isTrue();
		assertThat(check.test(name, ConfigurationPropertyName.adapt("MY_SPRINGBOOT_PROPERTY", '_'))).isTrue();
		assertThat(check.test(name, ConfigurationPropertyName.adapt("MY_BOOT_PROPERTY", '_'))).isFalse();
	}

	@Test
	void isAncestorOfWhenNonCanonicalSource() {
		ConfigurationPropertyName name = ConfigurationPropertyName.adapt("my.springBoot", '.');
		BiPredicate<ConfigurationPropertyName, ConfigurationPropertyName> check = getMapper().getAncestorOfCheck();
		assertThat(check.test(name, ConfigurationPropertyName.of("my.spring-boot.property"))).isTrue();
		assertThat(check.test(name, ConfigurationPropertyName.of("my.springboot.property"))).isTrue();
		assertThat(check.test(name, ConfigurationPropertyName.of("my.boot.property"))).isFalse();
	}

	@Test
	void isAncestorOfWhenNonCanonicalAndDashedSource() {
		ConfigurationPropertyName name = ConfigurationPropertyName.adapt("my.springBoot.input-value", '.');
		BiPredicate<ConfigurationPropertyName, ConfigurationPropertyName> check = getMapper().getAncestorOfCheck();
		assertThat(check.test(name, ConfigurationPropertyName.of("my.spring-boot.input-value.property"))).isTrue();
		assertThat(check.test(name, ConfigurationPropertyName.of("my.springboot.inputvalue.property"))).isTrue();
		assertThat(check.test(name, ConfigurationPropertyName.of("my.boot.property"))).isFalse();
	}

}
