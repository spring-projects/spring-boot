/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.context.properties.source;

import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.jspecify.annotations.Nullable;

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

	public ConfigurationPropertyNameAliases() {
	}

	public ConfigurationPropertyNameAliases(String name, String... aliases) {
		addAliases(name, aliases);
	}

	public ConfigurationPropertyNameAliases(ConfigurationPropertyName name, ConfigurationPropertyName... aliases) {
		addAliases(name, aliases);
	}

	public void addAliases(String name, String... aliases) {
		Assert.notNull(name, "'name' must not be null");
		Assert.notNull(aliases, "'aliases' must not be null");
		addAliases(ConfigurationPropertyName.of(name),
				Arrays.stream(aliases).map(ConfigurationPropertyName::of).toArray(ConfigurationPropertyName[]::new));
	}

	public void addAliases(ConfigurationPropertyName name, ConfigurationPropertyName... aliases) {
		Assert.notNull(name, "'name' must not be null");
		Assert.notNull(aliases, "'aliases' must not be null");
		this.aliases.addAll(name, Arrays.asList(aliases));
	}

	public List<ConfigurationPropertyName> getAliases(ConfigurationPropertyName name) {
		return this.aliases.getOrDefault(name, Collections.emptyList());
	}

	public @Nullable ConfigurationPropertyName getNameForAlias(ConfigurationPropertyName alias) {
		return this.aliases.entrySet()
			.stream()
			.filter((e) -> e.getValue().contains(alias))
			.map(Map.Entry::getKey)
			.findFirst()
			.orElse(null);
	}

	@Override
	public Iterator<ConfigurationPropertyName> iterator() {
		return this.aliases.keySet().iterator();
	}

}
