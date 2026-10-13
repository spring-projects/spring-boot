/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.context.properties.source;

import java.util.Arrays;
import java.util.function.Predicate;
import java.util.stream.Stream;

import org.jspecify.annotations.Nullable;

/**
 * A filtered {@link IterableConfigurationPropertySource}.
 *
 * @author Phillip Webb
 * @author Madhura Bhave
 */
class FilteredIterableConfigurationPropertiesSource extends FilteredConfigurationPropertiesSource
		implements IterableConfigurationPropertySource {

	private ConfigurationPropertyName @Nullable [] filteredNames;

	private int numberOfFilteredNames;

	FilteredIterableConfigurationPropertiesSource(IterableConfigurationPropertySource source,
			Predicate<ConfigurationPropertyName> filter) {
		super(source, filter);
		@Nullable ConfigurationPropertyName[] filterableNames = getFilterableNames(source);
		if (filterableNames != null) {
			this.filteredNames = new ConfigurationPropertyName[filterableNames.length];
			this.numberOfFilteredNames = 0;
			for (ConfigurationPropertyName name : filterableNames) {
				if (name == null) {
					break;
				}
				if (filter.test(name)) {
					this.filteredNames[this.numberOfFilteredNames++] = name;
				}
			}
		}
	}

	private @Nullable ConfigurationPropertyName @Nullable [] getFilterableNames(
			IterableConfigurationPropertySource source) {
		if (source instanceof SpringIterableConfigurationPropertySource springPropertySource
				&& springPropertySource.isImmutablePropertySource()) {
			return springPropertySource.getConfigurationPropertyNames();
		}
		if (source instanceof FilteredIterableConfigurationPropertiesSource filteredSource) {
			return filteredSource.filteredNames;
		}
		return null;
	}

	@Override
	public Stream<ConfigurationPropertyName> stream() {
		if (this.filteredNames != null) {
			return Arrays.stream(this.filteredNames, 0, this.numberOfFilteredNames);
		}
		return getSource().stream().filter(getFilter());
	}

	@Override
	protected IterableConfigurationPropertySource getSource() {
		return (IterableConfigurationPropertySource) super.getSource();
	}

	@Override
	public ConfigurationPropertyState containsDescendantOf(ConfigurationPropertyName name) {
		if (this.filteredNames != null) {
			return ConfigurationPropertyState.search(this.filteredNames, 0, this.numberOfFilteredNames,
					name::isAncestorOf);
		}
		return ConfigurationPropertyState.search(this, name::isAncestorOf);
	}

}
