/*
 * Copyright 2012-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.context.properties.migrator;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.springframework.boot.configurationmetadata.ConfigurationMetadataProperty;
import org.springframework.boot.configurationmetadata.ConfigurationMetadataRepository;
import org.springframework.boot.configurationmetadata.Deprecation;
import org.springframework.boot.context.properties.source.ConfigurationProperty;
import org.springframework.boot.context.properties.source.ConfigurationPropertyName;
import org.springframework.boot.context.properties.source.ConfigurationPropertySource;
import org.springframework.boot.context.properties.source.ConfigurationPropertySources;
import org.springframework.boot.env.OriginTrackedMapPropertySource;
import org.springframework.boot.origin.OriginTrackedValue;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.PropertySource;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;

/**
 * Report on {@link PropertyMigration properties migration}.
 *
 * @author Stephane Nicoll
 */
class PropertiesMigrationReporter {

	private final Map<String, ConfigurationMetadataProperty> allProperties;

	private final ConfigurableEnvironment environment;

	PropertiesMigrationReporter(ConfigurationMetadataRepository metadataRepository,
			ConfigurableEnvironment environment) {
		this.allProperties = Collections
				.unmodifiableMap(metadataRepository.getAllProperties());
		this.environment = environment;
	}

	/**
	 * Analyse the {@link ConfigurableEnvironment environment} and attempt to rename
	 * legacy properties if a replacement exists.
	 * @return a report of the migration
	 */
	public PropertiesMigrationReport getReport() {
		PropertiesMigrationReport report = new PropertiesMigrationReport();
		Map<String, List<PropertyMigration>> properties = getMatchingProperties(
				deprecatedFilter());
		if (properties.isEmpty()) {
			return report;
		}
		properties.forEach((name, candidates) -> {
			PropertySource<?> propertySource = mapPropertiesWithReplacement(report, name,
					candidates);
			if (propertySource != null) {
				this.environment.getPropertySources().addBefore(name, propertySource);
			}
		});
		return report;
	}

	private PropertySource<?> mapPropertiesWithReplacement(
			PropertiesMigrationReport report, String name,
			List<PropertyMigration> properties) {
		report.add(name, properties);
		List<PropertyMigration> renamed = properties.stream()
				.filter(PropertyMigration::isCompatibleType).collect(Collectors.toList());
		if (renamed.isEmpty()) {
			return null;
		}
		String target = "migrate-" + name;
		Map<String, OriginTrackedValue> content = new LinkedHashMap<>();
		for (PropertyMigration candidate : renamed) {
			OriginTrackedValue value = OriginTrackedValue.of(
					candidate.getProperty().getValue(),
					candidate.getProperty().getOrigin());
			content.put(candidate.getMetadata().getDeprecation().getReplacement(), value);
		}
		return new OriginTrackedMapPropertySource(target, content);
	}

	private Map<String, List<PropertyMigration>> getMatchingProperties(
			Predicate<ConfigurationMetadataProperty> filter) {
		MultiValueMap<String, PropertyMigration> result = new LinkedMultiValueMap<>();
		List<ConfigurationMetadataProperty> candidates = this.allProperties.values()
				.stream().filter(filter).collect(Collectors.toList());
		getPropertySourcesAsMap().forEach((name, source) -> {
			candidates.forEach((metadata) -> {
				ConfigurationProperty configurationProperty = source
						.getConfigurationProperty(
								ConfigurationPropertyName.of(metadata.getId()));
				if (configurationProperty != null) {
					result.add(name, new PropertyMigration(configurationProperty,
							metadata, determineReplacementMetadata(metadata)));
				}
			});
		});
		return result;
	}

	private ConfigurationMetadataProperty determineReplacementMetadata(
			ConfigurationMetadataProperty metadata) {
		String replacementId = metadata.getDeprecation().getReplacement();
		if (StringUtils.hasText(replacementId)) {
			ConfigurationMetadataProperty replacement = this.allProperties
					.get(replacementId);
			if (replacement != null) {
				return replacement;
			}
			return detectMapValueReplacement(replacementId);
		}
		return null;
	}

	private ConfigurationMetadataProperty detectMapValueReplacement(String fullId) {
		int lastDot = fullId.lastIndexOf('.');
		if (lastDot != -1) {
			return this.allProperties.get(fullId.substring(0, lastDot));
		}
		return null;
	}

	private Predicate<ConfigurationMetadataProperty> deprecatedFilter() {
		return (property) -> property.getDeprecation() != null
				&& property.getDeprecation().getLevel() == Deprecation.Level.ERROR;
	}

	private Map<String, ConfigurationPropertySource> getPropertySourcesAsMap() {
		Map<String, ConfigurationPropertySource> map = new LinkedHashMap<>();
		for (ConfigurationPropertySource source : ConfigurationPropertySources
				.get(this.environment)) {
			map.put(determinePropertySourceName(source), source);
		}
		return map;
	}

	private String determinePropertySourceName(ConfigurationPropertySource source) {
		if (source.getUnderlyingSource() instanceof PropertySource) {
			return ((PropertySource<?>) source.getUnderlyingSource()).getName();
		}
		return source.getUnderlyingSource().toString();
	}

}
