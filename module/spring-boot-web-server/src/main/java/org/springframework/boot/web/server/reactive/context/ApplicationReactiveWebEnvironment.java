/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.web.server.reactive.context;

import org.jspecify.annotations.Nullable;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.context.properties.source.ConfigurationPropertySources;
import org.springframework.boot.web.context.reactive.StandardReactiveWebEnvironment;
import org.springframework.core.env.ConfigurablePropertyResolver;
import org.springframework.core.env.MutablePropertySources;

/**
 * {@link StandardReactiveWebEnvironment} for typical use in a typical
 * {@link SpringApplication}.
 *
 * @author Phillip Webb
 */
class ApplicationReactiveWebEnvironment extends StandardReactiveWebEnvironment {

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
