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
import java.nio.charset.StandardCharsets;
import java.util.function.Function;

import org.springframework.core.io.ByteArrayResource;

/**
 * {@link ConfigDataLoader} to load data from environment variables.
 *
 * @author Moritz Halbritter
 */
class EnvConfigDataLoader implements ConfigDataLoader<EnvConfigDataResource> {

	private final Function<String, String> readEnvVariable;

	EnvConfigDataLoader() {
		this.readEnvVariable = System::getenv;
	}

	EnvConfigDataLoader(Function<String, String> readEnvVariable) {
		this.readEnvVariable = readEnvVariable;
	}

	@Override
	public ConfigData load(ConfigDataLoaderContext context, EnvConfigDataResource resource)
			throws IOException, ConfigDataResourceNotFoundException {
		String content = this.readEnvVariable.apply(resource.getVariableName());
		if (content == null) {
			throw new ConfigDataResourceNotFoundException(resource);
		}
		String name = String.format("Environment variable '%s' via location '%s'", resource.getVariableName(),
				resource.getLocation());
		return new ConfigData(resource.getLoader().load(name, createResource(content)));
	}

	private ByteArrayResource createResource(String content) {
		return new ByteArrayResource(content.getBytes(StandardCharsets.UTF_8));
	}

}
