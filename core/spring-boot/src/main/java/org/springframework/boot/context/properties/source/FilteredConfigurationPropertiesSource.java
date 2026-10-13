/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.context.properties.source;

import java.util.function.Predicate;

import org.jspecify.annotations.Nullable;

import org.springframework.util.Assert;

/**
 * A filtered {@link ConfigurationPropertySource}.
 *
 * @author Phillip Webb
 * @author Madhura Bhave
 */
class FilteredConfigurationPropertiesSource implements ConfigurationPropertySource {

	private final ConfigurationPropertySource source;

	private final Predicate<ConfigurationPropertyName> filter;

	FilteredConfigurationPropertiesSource(ConfigurationPropertySource source,
			Predicate<ConfigurationPropertyName> filter) {
		Assert.notNull(source, "'source' must not be null");
		Assert.notNull(filter, "'filter' must not be null");
		this.source = source;
		this.filter = filter;
	}

	@Override
	public @Nullable ConfigurationProperty getConfigurationProperty(ConfigurationPropertyName name) {
		boolean filtered = getFilter().test(name);
		return filtered ? getSource().getConfigurationProperty(name) : null;
	}

	@Override
	public ConfigurationPropertyState containsDescendantOf(ConfigurationPropertyName name) {
		ConfigurationPropertyState result = this.source.containsDescendantOf(name);
		if (result == ConfigurationPropertyState.PRESENT) {
			// We can't be sure a contained descendant won't be filtered
			return ConfigurationPropertyState.UNKNOWN;
		}
		return result;
	}

	@Override
	public @Nullable Object getUnderlyingSource() {
		return this.source.getUnderlyingSource();
	}

	protected ConfigurationPropertySource getSource() {
		return this.source;
	}

	protected Predicate<ConfigurationPropertyName> getFilter() {
		return this.filter;
	}

	@Override
	public String toString() {
		return this.source.toString() + " (filtered)";
	}

}
