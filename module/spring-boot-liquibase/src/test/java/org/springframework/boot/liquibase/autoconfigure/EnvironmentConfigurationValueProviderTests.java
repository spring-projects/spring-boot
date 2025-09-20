/*
 * Copyright 2012-present the original author or authors.
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

package org.springframework.boot.liquibase.autoconfigure;

import liquibase.configuration.ProvidedValue;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;

class EnvironmentConfigurationValueProviderTests {

	@Test
	void precedenceIsBetweenDefaultsAndEnvVars() {
		var env = new MockEnvironment();
		var provider = new EnvironmentConfigurationValueProvider(env);
		assertThat(provider.getPrecedence()).isEqualTo(100);
	}

	@Test
	void returnsProvidedValueWhenExactPropertyPresent() {
		var env = new MockEnvironment().withProperty("spring.liquibase.properties.liquibase.duplicateFileMode", "WARN");
		var provider = new EnvironmentConfigurationValueProvider(env);

		ProvidedValue value = provider.getProvidedValue("liquibase.duplicateFileMode");

		assertThat(value).isNotNull();
		assertThat(String.valueOf(value.getValue())).isEqualTo("WARN");
		assertThat(value.getActualKey()).isEqualTo("liquibase.duplicateFileMode");
		assertThat(value.getSourceDescription())
			.contains("Spring Environment property 'spring.liquibase.properties.liquibase.duplicateFileMode'");
	}

	@Test
	void returnsNullWhenNoKeysProvided() {
		var env = new MockEnvironment();
		var provider = new EnvironmentConfigurationValueProvider(env);

		ProvidedValue value = provider.getProvidedValue((String[]) null);

		assertThat(value).isNull();
	}

	@Test
	void returnsNullWhenNoMatchingProperty() {
		var env = new MockEnvironment();
		var provider = new EnvironmentConfigurationValueProvider(env);

		ProvidedValue value = provider.getProvidedValue("liquibase.searchPath");

		assertThat(value).isNull();
	}

	@Test
	void skipsNullKeysAndResolvesFirstMatchingNonNull() {
		var env = new MockEnvironment()
			// Only the second alias is present
			.withProperty("spring.liquibase.properties.liquibase.searchPath", "classpath:/db");
		var provider = new EnvironmentConfigurationValueProvider(env);

		ProvidedValue value = provider.getProvidedValue(null, "liquibase.searchPath");

		assertThat(value).isNotNull();
		assertThat(String.valueOf(value.getValue())).isEqualTo("classpath:/db");
		assertThat(value.getActualKey()).isEqualTo("liquibase.searchPath");
	}

	@Test
	void doesNotApplyRelaxedBinding_exactKeyOnly() {
		var env = new MockEnvironment().withProperty("spring.liquibase.properties.liquibase.duplicate-file-mode",
				"WARN");
		var provider = new EnvironmentConfigurationValueProvider(env);

		// Request camelCase; should not match the kebab-case property
		ProvidedValue value = provider.getProvidedValue("liquibase.duplicateFileMode");

		assertThat(value).isNull();
	}

}
