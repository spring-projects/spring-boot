/*
 * Copyright 2012-2020 the original author or authors.
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
