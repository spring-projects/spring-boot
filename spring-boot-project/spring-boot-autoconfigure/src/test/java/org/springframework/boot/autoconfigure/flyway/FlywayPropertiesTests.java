/*
 * Copyright 2012-2019 the original author or authors.
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

package org.springframework.boot.autoconfigure.flyway;

import java.beans.PropertyDescriptor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.flywaydb.core.api.Location;
import org.flywaydb.core.api.MigrationVersion;
import org.flywaydb.core.api.configuration.ClassicConfiguration;
import org.flywaydb.core.api.configuration.Configuration;
import org.flywaydb.core.api.configuration.FluentConfiguration;
import org.junit.jupiter.api.Test;

import org.springframework.beans.BeanWrapper;
import org.springframework.beans.PropertyAccessorFactory;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link FlywayProperties}.
 *
 * @author Stephane Nicoll
 */
class FlywayPropertiesTests {

	@Test
	void defaultValuesAreConsistent() {
		FlywayProperties properties = new FlywayProperties();
		Configuration configuration = new FluentConfiguration();
		assertThat(properties.getLocations().stream().map(Location::new).toArray(Location[]::new))
				.isEqualTo(configuration.getLocations());
		assertThat(properties.getEncoding()).isEqualTo(configuration.getEncoding());
		assertThat(properties.getConnectRetries()).isEqualTo(configuration.getConnectRetries());
		assertThat(properties.getSchemas()).isEqualTo(Arrays.asList(configuration.getSchemas()));
		assertThat(properties.getTable()).isEqualTo(configuration.getTable());
		assertThat(properties.getBaselineDescription()).isEqualTo(configuration.getBaselineDescription());
		assertThat(MigrationVersion.fromVersion(properties.getBaselineVersion()))
				.isEqualTo(configuration.getBaselineVersion());
		assertThat(properties.getInstalledBy()).isEqualTo(configuration.getInstalledBy());
		assertThat(properties.getPlaceholders()).isEqualTo(configuration.getPlaceholders());
		assertThat(properties.getPlaceholderPrefix()).isEqualToIgnoringWhitespace(configuration.getPlaceholderPrefix());
		assertThat(properties.getPlaceholderSuffix()).isEqualTo(configuration.getPlaceholderSuffix());
		assertThat(properties.isPlaceholderReplacement()).isEqualTo(configuration.isPlaceholderReplacement());
		assertThat(properties.getSqlMigrationPrefix()).isEqualTo(configuration.getSqlMigrationPrefix());
		assertThat(properties.getSqlMigrationSuffixes())
				.isEqualTo(Arrays.asList(configuration.getSqlMigrationSuffixes()));
		assertThat(properties.getSqlMigrationSeparator()).isEqualTo(properties.getSqlMigrationSeparator());
		assertThat(properties.getRepeatableSqlMigrationPrefix())
				.isEqualTo(properties.getRepeatableSqlMigrationPrefix());
		assertThat(properties.getTarget()).isNull();
		assertThat(configuration.getTarget()).isNull();
		assertThat(configuration.getInitSql()).isNull();
		assertThat(properties.getInitSqls()).isEmpty();
		assertThat(configuration.isBaselineOnMigrate()).isEqualTo(properties.isBaselineOnMigrate());
		assertThat(configuration.isCleanDisabled()).isEqualTo(properties.isCleanDisabled());
		assertThat(configuration.isCleanOnValidationError()).isEqualTo(properties.isCleanOnValidationError());
		assertThat(configuration.isGroup()).isEqualTo(properties.isGroup());
		assertThat(configuration.isIgnoreMissingMigrations()).isEqualTo(properties.isIgnoreMissingMigrations());
		assertThat(configuration.isIgnoreIgnoredMigrations()).isEqualTo(properties.isIgnoreIgnoredMigrations());
		assertThat(configuration.isIgnorePendingMigrations()).isEqualTo(properties.isIgnorePendingMigrations());
		assertThat(configuration.isIgnoreFutureMigrations()).isEqualTo(properties.isIgnoreFutureMigrations());
		assertThat(configuration.isMixed()).isEqualTo(properties.isMixed());
		assertThat(configuration.isOutOfOrder()).isEqualTo(properties.isOutOfOrder());
		assertThat(configuration.isSkipDefaultCallbacks()).isEqualTo(properties.isSkipDefaultCallbacks());
		assertThat(configuration.isSkipDefaultResolvers()).isEqualTo(properties.isSkipDefaultResolvers());
		assertThat(configuration.isValidateOnMigrate()).isEqualTo(properties.isValidateOnMigrate());
	}

	@Test
	void expectedPropertiesAreManaged() {
		Map<String, PropertyDescriptor> properties = indexProperties(
				PropertyAccessorFactory.forBeanPropertyAccess(new FlywayProperties()));
		Map<String, PropertyDescriptor> configuration = indexProperties(
				PropertyAccessorFactory.forBeanPropertyAccess(new ClassicConfiguration()));
		// Properties specific settings
		ignoreProperties(properties, "url", "user", "password", "enabled", "checkLocation", "createDataSource");

		// High level object we can't set with properties
		ignoreProperties(configuration, "classLoader", "dataSource", "resolvers", "callbacks");
		// Properties we don't want to expose
		ignoreProperties(configuration, "resolversAsClassNames", "callbacksAsClassNames");
		// Handled by the conversion service
		ignoreProperties(configuration, "baselineVersionAsString", "encodingAsString", "locationsAsStrings",
				"targetAsString");
		// Handled as initSql array
		ignoreProperties(configuration, "initSql");
		ignoreProperties(properties, "initSqls");
		// Handled as dryRunOutput
		ignoreProperties(configuration, "dryRunOutputAsFile", "dryRunOutputAsFileName");
		// Deprecated
		ignoreProperties(configuration, "errorHandlers", "errorHandlersAsClassNames");
		List<String> configurationKeys = new ArrayList<>(configuration.keySet());
		Collections.sort(configurationKeys);
		List<String> propertiesKeys = new ArrayList<>(properties.keySet());
		Collections.sort(propertiesKeys);
		assertThat(configurationKeys).isEqualTo(propertiesKeys);
	}

	private void ignoreProperties(Map<String, ?> index, String... propertyNames) {
		for (String propertyName : propertyNames) {
			assertThat(index.remove(propertyName)).describedAs("Property to ignore should be present " + propertyName)
					.isNotNull();
		}
	}

	private Map<String, PropertyDescriptor> indexProperties(BeanWrapper beanWrapper) {
		Map<String, PropertyDescriptor> descriptor = new HashMap<>();
		for (PropertyDescriptor propertyDescriptor : beanWrapper.getPropertyDescriptors()) {
			descriptor.put(propertyDescriptor.getName(), propertyDescriptor);
		}
		ignoreProperties(descriptor, "class");
		return descriptor;
	}

}
