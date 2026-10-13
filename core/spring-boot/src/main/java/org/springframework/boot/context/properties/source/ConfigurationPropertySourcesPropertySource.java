/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.context.properties.source;

import org.jspecify.annotations.Nullable;

import org.springframework.boot.origin.Origin;
import org.springframework.boot.origin.OriginLookup;
import org.springframework.core.env.Environment;
import org.springframework.core.env.PropertyResolver;
import org.springframework.core.env.PropertySource;

/**
 * {@link PropertySource} that exposes {@link ConfigurationPropertySource} instances so
 * that they can be used with a {@link PropertyResolver} or added to the
 * {@link Environment}.
 *
 * @author Phillip Webb
 * @author Madhura Bhave
 */
class ConfigurationPropertySourcesPropertySource extends PropertySource<Iterable<ConfigurationPropertySource>>
		implements OriginLookup<String> {

	ConfigurationPropertySourcesPropertySource(String name, Iterable<ConfigurationPropertySource> source) {
		super(name, source);
	}

	@Override
	public boolean containsProperty(String name) {
		return findConfigurationProperty(name) != null;
	}

	@Override
	public @Nullable Object getProperty(String name) {
		ConfigurationProperty configurationProperty = findConfigurationProperty(name);
		return (configurationProperty != null) ? configurationProperty.getValue() : null;
	}

	@Override
	public @Nullable Origin getOrigin(String name) {
		return Origin.from(findConfigurationProperty(name));
	}

	private @Nullable ConfigurationProperty findConfigurationProperty(String name) {
		try {
			return findConfigurationProperty(ConfigurationPropertyName.of(name, true));
		}
		catch (Exception ex) {
			return null;
		}
	}

	@Nullable ConfigurationProperty findConfigurationProperty(@Nullable ConfigurationPropertyName name) {
		if (name == null) {
			return null;
		}
		for (ConfigurationPropertySource configurationPropertySource : getSource()) {
			ConfigurationProperty configurationProperty = configurationPropertySource.getConfigurationProperty(name);
			if (configurationProperty != null) {
				return configurationProperty;
			}
		}
		return null;
	}

}
