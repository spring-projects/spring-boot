/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.context.properties.source;

import java.util.stream.Stream;

/**
 * An iterable {@link PrefixedConfigurationPropertySource}.
 *
 * @author Madhura Bhave
 */
class PrefixedIterableConfigurationPropertySource extends PrefixedConfigurationPropertySource
		implements IterableConfigurationPropertySource {

	PrefixedIterableConfigurationPropertySource(IterableConfigurationPropertySource source, String prefix) {
		super(source, prefix);
	}

	@Override
	public Stream<ConfigurationPropertyName> stream() {
		return getSource().stream().map(this::stripPrefix);
	}

	private ConfigurationPropertyName stripPrefix(ConfigurationPropertyName name) {
		return (getPrefix().isAncestorOf(name)) ? name.subName(getPrefix().getNumberOfElements()) : name;
	}

	@Override
	protected IterableConfigurationPropertySource getSource() {
		return (IterableConfigurationPropertySource) super.getSource();
	}

}
