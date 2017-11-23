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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.springframework.lang.NonNull;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

/**
 * Maintains a mapping of {@link ConfigurationPropertyName} aliases.
 *
 * @author Phillip Webb
 * @author Madhura Bhave
 * @since 2.0.0
 * @see ConfigurationPropertySource#withAliases(ConfigurationPropertyNameAliases)
 */
public final class ConfigurationPropertyNameAliases {

	private final MultiValueMap<ConfigurationPropertyName, ConfigurationPropertyName> aliases = new LinkedMultiValueMap<>();

	public ConfigurationPropertyNameAliases() {
	}

	public ConfigurationPropertyNameAliases(String name, String... aliases) {
		addAliases(name, aliases);
	}

	public ConfigurationPropertyNameAliases(ConfigurationPropertyName name,
			ConfigurationPropertyName... aliases) {
		addAliases(name, aliases);
	}

	public void addAliases(@NonNull String name, @NonNull String... aliases) {
		addAliases(ConfigurationPropertyName.of(name),
				Arrays.stream(aliases).map(ConfigurationPropertyName::of)
						.toArray(ConfigurationPropertyName[]::new));
	}

	public void addAliases(@NonNull ConfigurationPropertyName name,
			@NonNull ConfigurationPropertyName... aliases) {
		this.aliases.addAll(name, Arrays.asList(aliases));
	}

	public List<ConfigurationPropertyName> getAliases(ConfigurationPropertyName name) {
		return this.aliases.getOrDefault(name, Collections.emptyList());
	}

	public ConfigurationPropertyName getNameForAlias(ConfigurationPropertyName alias) {
		return this.aliases.entrySet().stream()
				.filter((e) -> e.getValue().contains(alias)).map(Map.Entry::getKey)
				.findFirst().orElse(null);
	}

}
