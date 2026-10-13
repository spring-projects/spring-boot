/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.context.properties.source;

import java.util.List;
import java.util.stream.Stream;

import org.springframework.util.CollectionUtils;

/**
 * A {@link IterableConfigurationPropertySource} supporting name aliases.
 *
 * @author Phillip Webb
 * @author Madhura Bhave
 */
class AliasedIterableConfigurationPropertySource extends AliasedConfigurationPropertySource
		implements IterableConfigurationPropertySource {

	AliasedIterableConfigurationPropertySource(IterableConfigurationPropertySource source,
			ConfigurationPropertyNameAliases aliases) {
		super(source, aliases);
	}

	@Override
	public Stream<ConfigurationPropertyName> stream() {
		return getSource().stream().flatMap(this::addAliases);
	}

	private Stream<ConfigurationPropertyName> addAliases(ConfigurationPropertyName name) {
		Stream<ConfigurationPropertyName> names = Stream.of(name);
		List<ConfigurationPropertyName> aliases = getAliases().getAliases(name);
		if (CollectionUtils.isEmpty(aliases)) {
			return names;
		}
		return Stream.concat(names, aliases.stream());
	}

	@Override
	protected IterableConfigurationPropertySource getSource() {
		return (IterableConfigurationPropertySource) super.getSource();
	}

}
