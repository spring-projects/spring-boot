/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.context.config;

import java.io.IOException;
import java.util.List;

import org.springframework.core.env.PropertySource;

/**
 * {@link ConfigDataLoader} to load data from system environment variables.
 *
 * @author Moritz Halbritter
 */
class SystemEnvironmentConfigDataLoader implements ConfigDataLoader<SystemEnvironmentConfigDataResource> {

	@Override
	public ConfigData load(ConfigDataLoaderContext context, SystemEnvironmentConfigDataResource resource)
			throws IOException, ConfigDataResourceNotFoundException {
		List<PropertySource<?>> loaded = resource.load();
		if (loaded == null) {
			throw new ConfigDataResourceNotFoundException(resource);
		}
		return new ConfigData(loaded);
	}

}
