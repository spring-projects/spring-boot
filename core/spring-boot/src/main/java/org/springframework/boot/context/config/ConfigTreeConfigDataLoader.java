/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.context.config;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collections;

import org.springframework.boot.env.ConfigTreePropertySource;
import org.springframework.boot.env.ConfigTreePropertySource.Option;

/**
 * {@link ConfigDataLoader} for config tree locations.
 *
 * @author Madhura Bhave
 * @author Phillip Webb
 * @since 2.4.0
 */
public class ConfigTreeConfigDataLoader implements ConfigDataLoader<ConfigTreeConfigDataResource> {

	@Override
	public ConfigData load(ConfigDataLoaderContext context, ConfigTreeConfigDataResource resource)
			throws IOException, ConfigDataResourceNotFoundException {
		Path path = resource.getPath();
		ConfigDataResourceNotFoundException.throwIfDoesNotExist(resource, path);
		String name = "Config tree '" + path + "'";
		ConfigTreePropertySource source = new ConfigTreePropertySource(name, path, Option.AUTO_TRIM_TRAILING_NEW_LINE);
		return new ConfigData(Collections.singletonList(source));
	}

}
