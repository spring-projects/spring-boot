/*
 * Copyright 2012-2020 the original author or authors.
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
import org.springframework.boot.context.properties.source.IterableConfigurationPropertySource;
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
		this.allProperties = Collections.unmodifiableMap(metadataRepository.getAllProperties());
		this.environment = environment;
	}

	/**
	 * Analyse the {@link ConfigurableEnvironment environment} and attempt to rename
	 * legacy properties if a replacement exists.
	 * @return a report of the migration
	 */
	PropertiesMigrationReport getReport() {
		PropertiesMigrationReport report = new PropertiesMigrationReport();
		Map<String, List<PropertyMigration>> properties = getMatchingProperties(
				ConfigurationMetadataProperty::isDeprecated);
		if (properties.isEmpty()) {
			return report;
		}
		properties.forEach((name, candidates) -> {
			PropertySource<?> propertySource = mapPropertiesWithReplacement(report, name, candidates);
			if (propertySource != null) {
				this.environment.getPropertySources().addBefore(name, propertySource);
			}
		});
		return report;
	}

	private PropertySource<?> mapPropertiesWithReplacement(PropertiesMigrationReport report, String name,
			List<PropertyMigration> properties) {
		report.add(name, properties);
		List<PropertyMigration> renamed = properties.stream().filter(PropertyMigration::isCompatibleType)
				.collect(Collectors.toList());
		if (renamed.isEmpty()) {
			return null;
		}
		String target = "migrate-" + name;
		Map<String, OriginTrackedValue> content = new LinkedHashMap<>();
		for (PropertyMigration candidate : renamed) {
			OriginTrackedValue value = OriginTrackedValue.of(candidate.getProperty().getValue(),
					candidate.getProperty().getOrigin());
			content.put(candidate.getMetadata().getDeprecation().getReplacement(), value);
		}
		return new OriginTrackedMapPropertySource(target, content);
	}

	private Map<String, List<PropertyMigration>> getMatchingProperties(
			Predicate<ConfigurationMetadataProperty> filter) {
		MultiValueMap<String, PropertyMigration> result = new LinkedMultiValueMap<>();
		List<ConfigurationMetadataProperty> candidates = this.allProperties.values().stream().filter(filter)
				.collect(Collectors.toList());
		getPropertySourcesAsMap().forEach((name, source) -> candidates.forEach((metadata) -> {
			ConfigurationProperty configurationProperty = source
					.getConfigurationProperty(ConfigurationPropertyName.of(metadata.getId()));
			if (configurationProperty != null) {
				result.add(name,
						new PropertyMigration(configurationProperty, metadata, determineReplacementMetadata(metadata)));
			}
			getMatchingPropertiesForMapEntries(name, source, metadata, result);
		}));
		return result;
	}

	private void getMatchingPropertiesForMapEntries(String name, ConfigurationPropertySource source,
			ConfigurationMetadataProperty metadata, MultiValueMap<String, PropertyMigration> result) {
		if (source instanceof IterableConfigurationPropertySource)
			((IterableConfigurationPropertySource) source).iterator().forEachRemaining(configurationPropertyName -> {
				ConfigurationProperty configurationProperty = source.getConfigurationProperty(configurationPropertyName);
				if (configurationProperty != null) {
					ConfigurationMetadataProperty entryMetadata = generateMetadataForMapEntry(configurationPropertyName.toString(), metadata);
					result.add(name, new PropertyMigration(configurationProperty, entryMetadata,
							determineReplacementMetadata(entryMetadata)));
				}
			});
		}

	private ConfigurationMetadataProperty generateMetadataForMapEntry(String propertyName,
			ConfigurationMetadataProperty mapMetadata) {
		ConfigurationMetadataProperty newMetadata = new ConfigurationMetadataProperty();
		newMetadata.setName(propertyName);
		newMetadata.setId(propertyName);
		String entryName = propertyName.substring(propertyName.lastIndexOf('.'));
		Deprecation oldDeprecation = mapMetadata.getDeprecation();
		if (oldDeprecation != null) {
			Deprecation deprecation = new Deprecation();
			deprecation.setLevel(oldDeprecation.getLevel());
			deprecation.setReason(oldDeprecation.getReason());
			deprecation.setShortReason(oldDeprecation.getShortReason());
			deprecation.setReplacement(oldDeprecation.getReplacement() + entryName);
			newMetadata.setDeprecation(deprecation);
		}

		String type = mapMetadata.getType();
		if (type.startsWith(Map.class.getName())) {
			int lastComma = type.lastIndexOf(',');
			if (lastComma != -1) {
				newMetadata.setType(type.substring(lastComma + 1, type.length() - 1).trim());
			}
		}
		return newMetadata;
	}

	private ConfigurationMetadataProperty determineReplacementMetadata(ConfigurationMetadataProperty metadata) {
		String replacementId = metadata.getDeprecation().getReplacement();
		if (StringUtils.hasText(replacementId)) {
			ConfigurationMetadataProperty replacement = this.allProperties.get(replacementId);
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

	private Map<String, ConfigurationPropertySource> getPropertySourcesAsMap() {
		Map<String, ConfigurationPropertySource> map = new LinkedHashMap<>();
		for (ConfigurationPropertySource source : ConfigurationPropertySources.get(this.environment)) {
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
