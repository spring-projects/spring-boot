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

import java.util.Objects;

import org.springframework.boot.env.PropertySourceLoader;

/**
 * {@link ConfigDataResource} used by {@link EnvConfigDataLoader}.
 *
 * @author Moritz Halbritter
 */
class EnvConfigDataResource extends ConfigDataResource {

	private final ConfigDataLocation location;

	private final String variableName;

	private final PropertySourceLoader loader;

	EnvConfigDataResource(ConfigDataLocation location, String variableName, PropertySourceLoader loader) {
		super(location.isOptional());
		this.location = location;
		this.variableName = variableName;
		this.loader = loader;
	}

	ConfigDataLocation getLocation() {
		return this.location;
	}

	String getVariableName() {
		return this.variableName;
	}

	PropertySourceLoader getLoader() {
		return this.loader;
	}

	@Override
	public boolean equals(Object o) {
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		EnvConfigDataResource that = (EnvConfigDataResource) o;
		return Objects.equals(this.location, that.location) && Objects.equals(this.variableName, that.variableName)
				&& Objects.equals(this.loader, that.loader);
	}

	@Override
	public int hashCode() {
		return Objects.hash(this.location, this.variableName, this.loader);
	}

	@Override
	public String toString() {
		return "env variable [" + this.variableName + "]";
	}

}
