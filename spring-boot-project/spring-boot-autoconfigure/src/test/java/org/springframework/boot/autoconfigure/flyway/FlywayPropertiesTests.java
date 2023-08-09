/*
 * Copyright 2012-2023 the original author or authors.
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
import java.time.Duration;
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
 * @author Chris Bono
 */
class FlywayPropertiesTests {

	@Test
	void defaultValuesAreConsistent() {
		FlywayProperties properties = new FlywayProperties();
		Configuration configuration = new FluentConfiguration();
		assertThat(properties.isFailOnMissingLocations()).isEqualTo(configuration.isFailOnMissingLocations());
		assertThat(properties.getLocations().stream().map(Location::new).toArray(Location[]::new))
			.isEqualTo(configuration.getLocations());
		assertThat(properties.getEncoding()).isEqualTo(configuration.getEncoding());
		assertThat(properties.getConnectRetries()).isEqualTo(configuration.getConnectRetries());
		assertThat(properties.getConnectRetriesInterval()).extracting(Duration::getSeconds)
			.extracting(Long::intValue)
			.isEqualTo(configuration.getConnectRetriesInterval());
		assertThat(properties.getLockRetryCount()).isEqualTo(configuration.getLockRetryCount());
		assertThat(properties.getDefaultSchema()).isEqualTo(configuration.getDefaultSchema());
		assertThat(properties.getSchemas()).isEqualTo(Arrays.asList(configuration.getSchemas()));
		assertThat(properties.isCreateSchemas()).isEqualTo(configuration.isCreateSchemas());
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
		assertThat(properties.getSqlMigrationSuffixes()).containsExactly(configuration.getSqlMigrationSuffixes());
		assertThat(properties.getSqlMigrationSeparator()).isEqualTo(configuration.getSqlMigrationSeparator());
		assertThat(properties.getRepeatableSqlMigrationPrefix())
			.isEqualTo(configuration.getRepeatableSqlMigrationPrefix());
		assertThat(MigrationVersion.fromVersion(properties.getTarget())).isEqualTo(configuration.getTarget());
		assertThat(configuration.getInitSql()).isNull();
		assertThat(properties.getInitSqls()).isEmpty();
		assertThat(properties.isBaselineOnMigrate()).isEqualTo(configuration.isBaselineOnMigrate());
		assertThat(properties.isCleanDisabled()).isEqualTo(configuration.isCleanDisabled());
		assertThat(properties.isCleanOnValidationError()).isEqualTo(configuration.isCleanOnValidationError());
		assertThat(properties.isGroup()).isEqualTo(configuration.isGroup());
		assertThat(properties.isMixed()).isEqualTo(configuration.isMixed());
		assertThat(properties.isOutOfOrder()).isEqualTo(configuration.isOutOfOrder());
		assertThat(properties.isSkipDefaultCallbacks()).isEqualTo(configuration.isSkipDefaultCallbacks());
		assertThat(properties.isSkipDefaultResolvers()).isEqualTo(configuration.isSkipDefaultResolvers());
		assertThat(properties.isValidateMigrationNaming()).isEqualTo(configuration.isValidateMigrationNaming());
		assertThat(properties.isValidateOnMigrate()).isEqualTo(configuration.isValidateOnMigrate());
		assertThat(properties.getDetectEncoding()).isNull();
		assertThat(properties.getPlaceholderSeparator()).isEqualTo(configuration.getPlaceholderSeparator());
		assertThat(properties.getScriptPlaceholderPrefix()).isEqualTo(configuration.getScriptPlaceholderPrefix());
		assertThat(properties.getScriptPlaceholderSuffix()).isEqualTo(configuration.getScriptPlaceholderSuffix());
		assertThat(properties.isExecuteInTransaction()).isEqualTo(configuration.isExecuteInTransaction());
	}

	@Test
	void loggersIsOverriddenToSlf4j() {
		assertThat(new FluentConfiguration().getLoggers()).containsExactly("auto");
		assertThat(new FlywayProperties().getLoggers()).containsExactly("slf4j");
	}

	@Test
	void expectedPropertiesAreManaged() {
		Map<String, PropertyDescriptor> properties = indexProperties(
				PropertyAccessorFactory.forBeanPropertyAccess(new FlywayProperties()));
		Map<String, PropertyDescriptor> configuration = indexProperties(
				PropertyAccessorFactory.forBeanPropertyAccess(new ClassicConfiguration()));
		// Properties specific settings
		ignoreProperties(properties, "url", "driverClassName", "user", "password", "enabled");
		// Deprecated properties
		ignoreProperties(properties, "oracleKerberosCacheFile", "oracleSqlplus", "oracleSqlplusWarn",
				"oracleWalletLocation", "sqlServerKerberosLoginFile");
		// Properties that are managed by specific extensions
		ignoreProperties(properties, "oracle", "postgresql", "sqlserver");
		// High level object we can't set with properties
		ignoreProperties(configuration, "callbacks", "classLoader", "dataSource", "javaMigrations",
				"javaMigrationClassProvider", "pluginRegister", "resourceProvider", "resolvers");
		// Properties we don't want to expose
		ignoreProperties(configuration, "resolversAsClassNames", "callbacksAsClassNames", "driver", "modernConfig",
				"currentResolvedEnvironment", "reportFilename", "reportEnabled", "workingDirectory");
		// Handled by the conversion service
		ignoreProperties(configuration, "baselineVersionAsString", "encodingAsString", "locationsAsStrings",
				"targetAsString");
		// Handled as initSql array
		ignoreProperties(configuration, "initSql");
		ignoreProperties(properties, "initSqls");
		// Handled as dryRunOutput
		ignoreProperties(configuration, "dryRunOutputAsFile", "dryRunOutputAsFileName");
		// Handled as createSchemas
		ignoreProperties(configuration, "shouldCreateSchemas");
		// Getters for the DataSource settings rather than actual properties
		ignoreProperties(configuration, "databaseType", "password", "url", "user");
		// Properties not exposed by Flyway
		ignoreProperties(configuration, "failOnMissingTarget");
		List<String> configurationKeys = new ArrayList<>(configuration.keySet());
		Collections.sort(configurationKeys);
		List<String> propertiesKeys = new ArrayList<>(properties.keySet());
		Collections.sort(propertiesKeys);
		assertThat(configurationKeys).containsExactlyElementsOf(propertiesKeys);
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
