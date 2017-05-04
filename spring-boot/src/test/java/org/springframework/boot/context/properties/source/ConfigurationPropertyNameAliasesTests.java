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

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ConfigurationPropertyNameAliases}.
 *
 * @author Phillip Webb
 * @author Madhura Bhave
 */
public class ConfigurationPropertyNameAliasesTests {

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	@Test
	public void createWithStringWhenNullNameShouldThrowException() throws Exception {
		this.thrown.expect(IllegalArgumentException.class);
		this.thrown.expectMessage("Name must not be null");
		new ConfigurationPropertyNameAliases((String) null);
	}

	@Test
	public void createWithStringShouldAddMapping() throws Exception {
		ConfigurationPropertyNameAliases aliases = new ConfigurationPropertyNameAliases(
				"foo", "bar", "baz");
		assertThat(aliases.getAliases(ConfigurationPropertyName.of("foo")))
				.containsExactly(ConfigurationPropertyName.of("bar"),
						ConfigurationPropertyName.of("baz"));
	}

	@Test
	public void createWithNameShouldAddMapping() throws Exception {
		ConfigurationPropertyNameAliases aliases = new ConfigurationPropertyNameAliases(
				ConfigurationPropertyName.of("foo"), ConfigurationPropertyName.of("bar"),
				ConfigurationPropertyName.of("baz"));
		assertThat(aliases.getAliases(ConfigurationPropertyName.of("foo")))
				.containsExactly(ConfigurationPropertyName.of("bar"),
						ConfigurationPropertyName.of("baz"));
	}

	@Test
	public void addAliasesFromStringShouldAddMapping() throws Exception {
		ConfigurationPropertyNameAliases aliases = new ConfigurationPropertyNameAliases();
		aliases.addAliases("foo", "bar", "baz");
		assertThat(aliases.getAliases(ConfigurationPropertyName.of("foo")))
				.containsExactly(ConfigurationPropertyName.of("bar"),
						ConfigurationPropertyName.of("baz"));
	}

	@Test
	public void addAliasesFromNameShouldAddMapping() throws Exception {
		ConfigurationPropertyNameAliases aliases = new ConfigurationPropertyNameAliases();
		aliases.addAliases(ConfigurationPropertyName.of("foo"),
				ConfigurationPropertyName.of("bar"), ConfigurationPropertyName.of("baz"));
		assertThat(aliases.getAliases(ConfigurationPropertyName.of("foo")))
				.containsExactly(ConfigurationPropertyName.of("bar"),
						ConfigurationPropertyName.of("baz"));
	}

	@Test
	public void addWhenHasExistingShouldAddAdditionalMappings() throws Exception {
		ConfigurationPropertyNameAliases aliases = new ConfigurationPropertyNameAliases();
		aliases.addAliases("foo", "bar");
		aliases.addAliases("foo", "baz");
		assertThat(aliases.getAliases(ConfigurationPropertyName.of("foo")))
				.containsExactly(ConfigurationPropertyName.of("bar"),
						ConfigurationPropertyName.of("baz"));
	}

	@Test
	public void getAliasesWhenNotMappedShouldReturnEmptyList() throws Exception {
		ConfigurationPropertyNameAliases aliases = new ConfigurationPropertyNameAliases();
		assertThat(aliases.getAliases(ConfigurationPropertyName.of("foo"))).isEmpty();
	}

	@Test
	public void getAliasesWhenMappedShouldReturnMapping() throws Exception {
		ConfigurationPropertyNameAliases aliases = new ConfigurationPropertyNameAliases();
		aliases.addAliases("foo", "bar");
		assertThat(aliases.getAliases(ConfigurationPropertyName.of("foo")))
				.containsExactly(ConfigurationPropertyName.of("bar"));
	}

	@Test
	public void getNameForAliasWhenHasMappingShouldReturnName() throws Exception {
		ConfigurationPropertyNameAliases aliases = new ConfigurationPropertyNameAliases();
		aliases.addAliases("foo", "bar");
		aliases.addAliases("foo", "baz");
		assertThat((Object) aliases.getNameForAlias(ConfigurationPropertyName.of("bar")))
				.isEqualTo(ConfigurationPropertyName.of("foo"));
		assertThat((Object) aliases.getNameForAlias(ConfigurationPropertyName.of("baz")))
				.isEqualTo(ConfigurationPropertyName.of("foo"));
	}

	@Test
	public void getNameForAliasWhenNotMappedShouldReturnNull() throws Exception {
		ConfigurationPropertyNameAliases aliases = new ConfigurationPropertyNameAliases();
		aliases.addAliases("foo", "bar");
		assertThat((Object) aliases.getNameForAlias(ConfigurationPropertyName.of("baz")))
				.isNull();
	}

}
