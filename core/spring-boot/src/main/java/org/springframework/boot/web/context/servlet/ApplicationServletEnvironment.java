/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.web.context.servlet;

import org.jspecify.annotations.Nullable;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.context.properties.source.ConfigurationPropertySources;
import org.springframework.core.env.ConfigurablePropertyResolver;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.web.context.support.StandardServletEnvironment;

/**
 * {@link StandardServletEnvironment} for typical use in a typical
 * {@link SpringApplication}.
 *
 * @author Phillip Webb
 * @since 4.0.0
 */
public class ApplicationServletEnvironment extends StandardServletEnvironment {

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
