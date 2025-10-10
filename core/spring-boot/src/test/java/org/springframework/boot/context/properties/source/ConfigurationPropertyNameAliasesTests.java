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

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Tests for {@link ConfigurationPropertyNameAliases}.
 *
 * @author Phillip Webb
 * @author Madhura Bhave
 */
class ConfigurationPropertyNameAliasesTests {

	@Test
	@SuppressWarnings("NullAway") // Test null check
	void createWithStringWhenNullNameShouldThrowException() {
		assertThatIllegalArgumentException().isThrownBy(() -> new ConfigurationPropertyNameAliases((String) null))
			.withMessageContaining("'name' must not be null");
	}

	@Test
	void createWithStringShouldAddMapping() {
		ConfigurationPropertyNameAliases aliases = new ConfigurationPropertyNameAliases("foo", "bar", "baz");
		assertThat(aliases.getAliases(ConfigurationPropertyName.of("foo")))
			.containsExactly(ConfigurationPropertyName.of("bar"), ConfigurationPropertyName.of("baz"));
	}

	@Test
	void createWithNameShouldAddMapping() {
		ConfigurationPropertyNameAliases aliases = new ConfigurationPropertyNameAliases(
				ConfigurationPropertyName.of("foo"), ConfigurationPropertyName.of("bar"),
				ConfigurationPropertyName.of("baz"));
		assertThat(aliases.getAliases(ConfigurationPropertyName.of("foo")))
			.containsExactly(ConfigurationPropertyName.of("bar"), ConfigurationPropertyName.of("baz"));
	}

	@Test
	void addAliasesFromStringShouldAddMapping() {
		ConfigurationPropertyNameAliases aliases = new ConfigurationPropertyNameAliases();
		aliases.addAliases("foo", "bar", "baz");
		assertThat(aliases.getAliases(ConfigurationPropertyName.of("foo")))
			.containsExactly(ConfigurationPropertyName.of("bar"), ConfigurationPropertyName.of("baz"));
	}

	@Test
	void addAliasesFromNameShouldAddMapping() {
		ConfigurationPropertyNameAliases aliases = new ConfigurationPropertyNameAliases();
		aliases.addAliases(ConfigurationPropertyName.of("foo"), ConfigurationPropertyName.of("bar"),
				ConfigurationPropertyName.of("baz"));
		assertThat(aliases.getAliases(ConfigurationPropertyName.of("foo")))
			.containsExactly(ConfigurationPropertyName.of("bar"), ConfigurationPropertyName.of("baz"));
	}

	@Test
	void addWhenHasExistingShouldAddAdditionalMappings() {
		ConfigurationPropertyNameAliases aliases = new ConfigurationPropertyNameAliases();
		aliases.addAliases("foo", "bar");
		aliases.addAliases("foo", "baz");
		assertThat(aliases.getAliases(ConfigurationPropertyName.of("foo")))
			.containsExactly(ConfigurationPropertyName.of("bar"), ConfigurationPropertyName.of("baz"));
	}

	@Test
	void getAliasesWhenNotMappedShouldReturnEmptyList() {
		ConfigurationPropertyNameAliases aliases = new ConfigurationPropertyNameAliases();
		assertThat(aliases.getAliases(ConfigurationPropertyName.of("foo"))).isEmpty();
	}

	@Test
	void getAliasesWhenMappedShouldReturnMapping() {
		ConfigurationPropertyNameAliases aliases = new ConfigurationPropertyNameAliases();
		aliases.addAliases("foo", "bar");
		assertThat(aliases.getAliases(ConfigurationPropertyName.of("foo")))
			.containsExactly(ConfigurationPropertyName.of("bar"));
	}

	@Test
	void getNameForAliasWhenHasMappingShouldReturnName() {
		ConfigurationPropertyNameAliases aliases = new ConfigurationPropertyNameAliases();
		aliases.addAliases("foo", "bar");
		aliases.addAliases("foo", "baz");
		assertThat((Object) aliases.getNameForAlias(ConfigurationPropertyName.of("bar")))
			.isEqualTo(ConfigurationPropertyName.of("foo"));
		assertThat((Object) aliases.getNameForAlias(ConfigurationPropertyName.of("baz")))
			.isEqualTo(ConfigurationPropertyName.of("foo"));
	}

	@Test
	void getNameForAliasWhenNotMappedShouldReturnNull() {
		ConfigurationPropertyNameAliases aliases = new ConfigurationPropertyNameAliases();
		aliases.addAliases("foo", "bar");
		assertThat((Object) aliases.getNameForAlias(ConfigurationPropertyName.of("baz"))).isNull();
	}

}
