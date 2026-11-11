/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot;

import org.jspecify.annotations.Nullable;

import org.springframework.boot.context.properties.source.ConfigurationPropertySources;
import org.springframework.core.env.ConfigurablePropertyResolver;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.StandardEnvironment;

/**
 * {@link StandardEnvironment} for typical use in a typical {@link SpringApplication}.
 *
 * @author Phillip Webb
 */
class ApplicationEnvironment extends StandardEnvironment {

	@Override
	protected @Nullable String doGetActiveProfilesProperty() {
		return null;
	}

	@Override
	protected @Nullable String doGetDefaultProfilesProperty() {
		return null;
	}

	@Override
	protected ConfigurablePropertyResolver createPropertyResolver(MutablePropertySources propertySources) {
		return ConfigurationPropertySources.createPropertyResolver(propertySources);
	}

}
