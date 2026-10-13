/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.context.properties.source;

import org.jspecify.annotations.Nullable;

import org.springframework.util.Assert;

/**
 * A {@link ConfigurationPropertySource} supporting a prefix.
 *
 * @author Madhura Bhave
 */
class PrefixedConfigurationPropertySource implements ConfigurationPropertySource {

	private final ConfigurationPropertySource source;

	private final ConfigurationPropertyName prefix;

	PrefixedConfigurationPropertySource(ConfigurationPropertySource source, String prefix) {
		Assert.notNull(source, "'source' must not be null");
		Assert.hasText(prefix, "'prefix' must not be empty");
		this.source = source;
		this.prefix = ConfigurationPropertyName.of(prefix);
	}

	protected final ConfigurationPropertyName getPrefix() {
		return this.prefix;
	}

	@Override
	public @Nullable ConfigurationProperty getConfigurationProperty(ConfigurationPropertyName name) {
		ConfigurationProperty configurationProperty = this.source.getConfigurationProperty(getPrefixedName(name));
		if (configurationProperty == null) {
			return null;
		}
		return ConfigurationProperty.of(configurationProperty.getSource(), name, configurationProperty.getValue(),
				configurationProperty.getOrigin());
	}

	private ConfigurationPropertyName getPrefixedName(ConfigurationPropertyName name) {
		return this.prefix.append(name);
	}

	@Override
	public ConfigurationPropertyState containsDescendantOf(ConfigurationPropertyName name) {
		return this.source.containsDescendantOf(getPrefixedName(name));
	}

	@Override
	public @Nullable Object getUnderlyingSource() {
		return this.source.getUnderlyingSource();
	}

	protected ConfigurationPropertySource getSource() {
		return this.source;
	}

}
