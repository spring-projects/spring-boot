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

import org.junit.jupiter.api.Test;

import org.springframework.boot.env.PropertiesPropertySourceLoader;
import org.springframework.boot.env.PropertySourceLoader;
import org.springframework.boot.env.YamlPropertySourceLoader;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link EnvConfigDataResource}.
 *
 * @author Moritz Halbritter
 */
class EnvConfigDataResourceTests {

	private final YamlPropertySourceLoader yamlPropertySourceLoader = new YamlPropertySourceLoader();

	private final PropertiesPropertySourceLoader propertiesPropertySourceLoader = new PropertiesPropertySourceLoader();

	@Test
	void shouldHaveEqualsAndHashcode() {
		EnvConfigDataResource var1 = createResource("VAR1");
		EnvConfigDataResource var2 = createResource("VAR2");
		EnvConfigDataResource var3 = createResource("VAR1", this.yamlPropertySourceLoader);
		EnvConfigDataResource var4 = createResource("VAR1");
		assertThat(var1).isNotEqualTo(var2);
		assertThat(var1).isNotEqualTo(var3);
		assertThat(var1).isEqualTo(var4);
		assertThat(var1).hasSameHashCodeAs(var4);
	}

	@Test
	void shouldHaveToString() {
		EnvConfigDataResource resource = createResource("VAR1");
		assertThat(resource).hasToString("env variable [VAR1]");
	}

	private EnvConfigDataResource createResource(String variableName) {
		return createResource(variableName, this.propertiesPropertySourceLoader);
	}

	private EnvConfigDataResource createResource(String variableName, PropertySourceLoader propertySourceLoader) {
		return new EnvConfigDataResource(ConfigDataLocation.of("env:" + variableName), variableName,
				propertySourceLoader);
	}

}
