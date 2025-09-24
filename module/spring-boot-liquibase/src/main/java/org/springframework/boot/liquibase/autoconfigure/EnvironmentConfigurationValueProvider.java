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

import liquibase.configuration.AbstractConfigurationValueProvider;
import liquibase.configuration.ProvidedValue;
import org.jspecify.annotations.Nullable;
import org.springframework.core.env.Environment;
import org.springframework.util.Assert;

/**
 * A Liquibase {@code ConfigurationValueProvider} that passes through properties defined
 * in the Spring {@link Environment} using the exact-name convention:
 *
 * <pre>
 * spring.liquibase.properties.&lt;liquibaseKey&gt; = &lt;value&gt;
 * </pre>
 *
 * For example: <pre>
 * spring.liquibase.properties.liquibase.duplicateFileMode = WARN
 * spring.liquibase.properties.liquibase.searchPath = classpath:/db,file:./external
 * </pre>
 *
 * No relaxed binding or key transformation is performed. Keys are looked up exactly as
 * provided by Liquibase (including dots and casing), prefixed with
 * {@code spring.liquibase.properties.}.
 */
final class EnvironmentConfigurationValueProvider extends AbstractConfigurationValueProvider {

	private static final String PREFIX = "spring.liquibase.properties.";

	private final Environment environment;

	EnvironmentConfigurationValueProvider(Environment environment) {
		Assert.notNull(environment, "Environment must not be null");
		this.environment = environment;
	}

	@Override
	public int getPrecedence() {
		return 100;
	}

	@Override
	public @Nullable ProvidedValue getProvidedValue(String... keyAndAliases) {
		if (keyAndAliases == null) {
			return null;
		}
		for (String requestedKey : keyAndAliases) {
			if (requestedKey == null) {
				continue;
			}
			String propertyName = PREFIX + requestedKey;
			String value = this.environment.getProperty(propertyName);
			if (value != null) {
				return new ProvidedValue(requestedKey, requestedKey, value,
						"Spring Environment property '" + propertyName + "'", this);
			}
		}
		return null;
	}

}
