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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import org.springframework.boot.env.PropertiesPropertySourceLoader;
import org.springframework.boot.env.PropertySourceLoader;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.PropertySource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link SystemEnvironmentConfigDataResource}.
 *
 * @author Moritz Halbritter
 */
class SystemEnvironmentConfigDataResourceTests {

	private Map<String, String> environment = new HashMap<>();

	private final YamlPropertySourceLoader yamlLoader = new YamlPropertySourceLoader();

	private final PropertiesPropertySourceLoader propertiesLoader = new PropertiesPropertySourceLoader();

	@Test
	void loadLoadsPropertySources() throws IOException {
		this.environment.put("VAR1", "key1=value1");
		List<PropertySource<?>> loaded = createResource("VAR1").load();
		assertThat(loaded).hasSize(1);
		assertThat(loaded.get(0).getProperty("key1")).isEqualTo("value1");
	}

	@Test
	void loadWhenNoContentReturnsNull() throws IOException {
		List<PropertySource<?>> loaded = createResource("VAR1").load();
		assertThat(loaded).isNull();
	}

	@Test
	void equalsAndHashcode() {
		SystemEnvironmentConfigDataResource var1 = createResource("VAR1");
		SystemEnvironmentConfigDataResource var2 = createResource("VAR2");
		SystemEnvironmentConfigDataResource var3 = createResource("VAR1", this.yamlLoader);
		SystemEnvironmentConfigDataResource var4 = createResource("VAR1");
		assertThat(var1).isNotEqualTo(var2);
		assertThat(var1).isNotEqualTo(var3);
		assertThat(var1).isEqualTo(var4);
		assertThat(var1).hasSameHashCodeAs(var4);
	}

	@Test
	void toStringReturnsString() {
		SystemEnvironmentConfigDataResource resource = createResource("VAR1");
		assertThat(resource)
			.hasToString("system envionement variable [VAR1] content loaded using PropertiesPropertySourceLoader");
	}

	private SystemEnvironmentConfigDataResource createResource(String variableName) {
		return createResource(variableName, this.propertiesLoader);
	}

	private SystemEnvironmentConfigDataResource createResource(String variableName,
			PropertySourceLoader propertySourceLoader) {
		return new SystemEnvironmentConfigDataResource(variableName, propertySourceLoader, this.environment::get);
	}

}
