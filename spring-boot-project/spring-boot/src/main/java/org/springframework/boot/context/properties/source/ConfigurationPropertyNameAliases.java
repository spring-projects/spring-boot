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

import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.springframework.util.Assert;
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
public final class ConfigurationPropertyNameAliases implements Iterable<ConfigurationPropertyName> {

	private final MultiValueMap<ConfigurationPropertyName, ConfigurationPropertyName> aliases = new LinkedMultiValueMap<>();

	/**
     * Constructs a new ConfigurationPropertyNameAliases object.
     */
    public ConfigurationPropertyNameAliases() {
	}

	/**
     * Constructs a new ConfigurationPropertyNameAliases object with the specified name and aliases.
     * 
     * @param name the name of the property
     * @param aliases the aliases for the property
     */
    public ConfigurationPropertyNameAliases(String name, String... aliases) {
		addAliases(name, aliases);
	}

	/**
     * Constructs a new ConfigurationPropertyNameAliases object with the specified name and aliases.
     * 
     * @param name the main configuration property name
     * @param aliases the aliases for the configuration property name
     */
    public ConfigurationPropertyNameAliases(ConfigurationPropertyName name, ConfigurationPropertyName... aliases) {
		addAliases(name, aliases);
	}

	/**
     * Adds aliases for a given name.
     * 
     * @param name the name for which aliases are to be added (must not be null)
     * @param aliases the aliases to be added (must not be null)
     * @throws IllegalArgumentException if name or aliases is null
     */
    public void addAliases(String name, String... aliases) {
		Assert.notNull(name, "Name must not be null");
		Assert.notNull(aliases, "Aliases must not be null");
		addAliases(ConfigurationPropertyName.of(name),
				Arrays.stream(aliases).map(ConfigurationPropertyName::of).toArray(ConfigurationPropertyName[]::new));
	}

	/**
     * Adds aliases for a given configuration property name.
     * 
     * @param name the configuration property name to add aliases for (must not be null)
     * @param aliases the aliases to be added (must not be null)
     * @throws IllegalArgumentException if either the name or aliases are null
     */
    public void addAliases(ConfigurationPropertyName name, ConfigurationPropertyName... aliases) {
		Assert.notNull(name, "Name must not be null");
		Assert.notNull(aliases, "Aliases must not be null");
		this.aliases.addAll(name, Arrays.asList(aliases));
	}

	/**
     * Retrieves the aliases associated with the given configuration property name.
     * 
     * @param name the configuration property name
     * @return a list of configuration property names that are aliases of the given name,
     *         or an empty list if no aliases are found
     */
    public List<ConfigurationPropertyName> getAliases(ConfigurationPropertyName name) {
		return this.aliases.getOrDefault(name, Collections.emptyList());
	}

	/**
     * Returns the original configuration property name for the given alias.
     * 
     * @param alias the alias configuration property name
     * @return the original configuration property name, or null if not found
     */
    public ConfigurationPropertyName getNameForAlias(ConfigurationPropertyName alias) {
		return this.aliases.entrySet()
			.stream()
			.filter((e) -> e.getValue().contains(alias))
			.map(Map.Entry::getKey)
			.findFirst()
			.orElse(null);
	}

	/**
     * Returns an iterator over the elements in this ConfigurationPropertyNameAliases object.
     *
     * @return an iterator over the elements in this ConfigurationPropertyNameAliases object
     */
    @Override
	public Iterator<ConfigurationPropertyName> iterator() {
		return this.aliases.keySet().iterator();
	}

}
