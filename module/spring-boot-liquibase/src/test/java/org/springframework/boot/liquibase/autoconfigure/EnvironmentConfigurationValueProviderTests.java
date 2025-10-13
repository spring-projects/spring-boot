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

import liquibase.Scope;
import liquibase.configuration.ProvidedValue;
import org.junit.jupiter.api.Test;
import org.springframework.core.env.Environment;
import org.springframework.mock.env.MockEnvironment;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link EnvironmentConfigurationValueProvider}.
 *
 * @author Dylan Miska
 */
class EnvironmentConfigurationValueProviderTests {

	@Test
	void precedenceIsBetweenDefaultsAndEnvVars() {
		EnvironmentConfigurationValueProvider provider = new EnvironmentConfigurationValueProvider();
		assertThat(provider.getPrecedence()).isEqualTo(100);
	}

	@Test
	void returnsProvidedValueWhenExactPropertyPresent() throws Exception {
		MockEnvironment env = new MockEnvironment()
			.withProperty("spring.liquibase.properties.liquibase.duplicateFileMode", "WARN");
		runInScope(env, () -> {
			EnvironmentConfigurationValueProvider provider = new EnvironmentConfigurationValueProvider();

			ProvidedValue value = provider.getProvidedValue("liquibase.duplicateFileMode");

			assertThat(value).isNotNull();
			assertThat(String.valueOf(value.getValue())).isEqualTo("WARN");
			assertThat(value.getActualKey()).isEqualTo("liquibase.duplicateFileMode");
			assertThat(value.getSourceDescription())
				.contains("Spring Environment property 'spring.liquibase.properties.liquibase.duplicateFileMode'");
		});
	}

	@Test
	void returnsNullWhenNoMatchingProperty() throws Exception {
		MockEnvironment env = new MockEnvironment();
		runInScope(env, () -> {
			EnvironmentConfigurationValueProvider provider = new EnvironmentConfigurationValueProvider();

			ProvidedValue value = provider.getProvidedValue("liquibase.searchPath");

			assertThat(value).isNull();
		});
	}

	@Test
	void returnsNullWhenCalledOutsideOfLiquibaseScope() {
		// Register an environment, but call getProvidedValue outside of a Liquibase scope
		MockEnvironment env = new MockEnvironment()
			.withProperty("spring.liquibase.properties.liquibase.duplicateFileMode", "WARN");
		String environmentId = UUID.randomUUID().toString();
		EnvironmentConfigurationValueProvider.registerEnvironment(environmentId, env);
		try {
			EnvironmentConfigurationValueProvider provider = new EnvironmentConfigurationValueProvider();

			// Call outside of Scope.child - no SPRING_ENV_ID_KEY in current scope
			ProvidedValue value = provider.getProvidedValue("liquibase.duplicateFileMode");

			assertThat(value).isNull();
		}
		finally {
			EnvironmentConfigurationValueProvider.unregisterEnvironment(environmentId);
		}
	}

	@Test
	void returnsNullWhenInsideLiquibaseScopeButEnvironmentNotRegistered() throws Exception {
		EnvironmentConfigurationValueProvider provider = new EnvironmentConfigurationValueProvider();

		// Create a Liquibase scope with an environment ID that doesn't exist in the
		// registry
		String nonExistentId = UUID.randomUUID().toString();
		Map<String, Object> scopeValues = new HashMap<>();
		scopeValues.put(EnvironmentConfigurationValueProvider.SPRING_ENV_ID_KEY, nonExistentId);

		Scope.child(scopeValues, () -> {
			ProvidedValue value = provider.getProvidedValue("liquibase.duplicateFileMode");

			assertThat(value).isNull();
		});
	}

	@Test
	void doesNotApplyRelaxedBinding_exactKeyOnly() throws Exception {
		MockEnvironment env = new MockEnvironment()
			.withProperty("spring.liquibase.properties.liquibase.duplicate-file-mode", "WARN");
		runInScope(env, () -> {
			EnvironmentConfigurationValueProvider provider = new EnvironmentConfigurationValueProvider();

			// Request camelCase; should not match the kebab-case property
			ProvidedValue value = provider.getProvidedValue("liquibase.duplicateFileMode");

			assertThat(value).isNull();
		});
	}

	private void runInScope(Environment environment, Runnable runnable) throws Exception {
		String environmentId = UUID.randomUUID().toString();
		EnvironmentConfigurationValueProvider.registerEnvironment(environmentId, environment);
		try {
			Map<String, Object> scopeValues = new HashMap<>();
			scopeValues.put(EnvironmentConfigurationValueProvider.SPRING_ENV_ID_KEY, environmentId);
			Scope.child(scopeValues, () -> {
				runnable.run();
			});
		}
		finally {
			EnvironmentConfigurationValueProvider.unregisterEnvironment(environmentId);
		}
	}

}
