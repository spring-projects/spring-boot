/*
 * Copyright 2012-2025 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
