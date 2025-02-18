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
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

import org.springframework.boot.env.PropertySourceLoader;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

/**
 * {@link ConfigDataResource} used by {@link SystemEnvironmentConfigDataLoader}.
 *
 * @author Moritz Halbritter
 */
class SystemEnvironmentConfigDataResource extends ConfigDataResource {

	private final String variableName;

	private final PropertySourceLoader loader;

	private final Function<String, String> environment;

	SystemEnvironmentConfigDataResource(String variableName, PropertySourceLoader loader,
			Function<String, String> environment) {
		this.variableName = variableName;
		this.loader = loader;
		this.environment = environment;
	}

	String getVariableName() {
		return this.variableName;
	}

	PropertySourceLoader getLoader() {
		return this.loader;
	}

	List<PropertySource<?>> load() throws IOException {
		String content = this.environment.apply(this.variableName);
		return (content != null) ? this.loader.load(StringUtils.capitalize(toString()), asResource(content)) : null;
	}

	private ByteArrayResource asResource(String content) {
		return new ByteArrayResource(content.getBytes(StandardCharsets.UTF_8));
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null || getClass() != obj.getClass()) {
			return false;
		}
		SystemEnvironmentConfigDataResource other = (SystemEnvironmentConfigDataResource) obj;
		return Objects.equals(this.loader.getClass(), other.loader.getClass())
				&& Objects.equals(this.variableName, other.variableName);
	}

	@Override
	public int hashCode() {
		return Objects.hash(this.variableName, this.loader.getClass());
	}

	@Override
	public String toString() {
		return "system envionement variable [" + this.variableName + "] content loaded using "
				+ ClassUtils.getShortName(this.loader.getClass());
	}

}
