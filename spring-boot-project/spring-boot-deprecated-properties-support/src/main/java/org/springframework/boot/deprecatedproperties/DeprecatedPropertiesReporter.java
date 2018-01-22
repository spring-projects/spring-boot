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

package org.springframework.boot.deprecatedproperties;

import java.util.ArrayList;
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
 * Report on {@link DeprecatedProperty deprecated properties}.
 *
 * @author Stephane Nicoll
 */
class DeprecatedPropertiesReporter {

	private final Map<String, ConfigurationMetadataProperty> allProperties;

	private final ConfigurableEnvironment environment;

	DeprecatedPropertiesReporter(ConfigurationMetadataRepository metadataRepository,
			ConfigurableEnvironment environment) {
		this.allProperties = Collections
				.unmodifiableMap(metadataRepository.getAllProperties());
		this.environment = environment;
	}

	/**
	 * Analyse the {@link ConfigurableEnvironment environment} and attempt to rename
	 * legacy properties if a replacement exists.
	 * @return the analysis
	 */
	public DeprecatedPropertiesReport getReport() {
		DeprecatedPropertiesReport report = new DeprecatedPropertiesReport();
		Map<String, List<DeprecatedProperty>> properties = getMatchingProperties(
				deprecatedFilter());
		if (properties.isEmpty()) {
			return report;
		}
		properties.forEach((name, candidates) -> {
			PropertySource<?> propertySource = mapPropertiesWithReplacement(report,
					name, candidates);
			if (propertySource != null) {
				this.environment.getPropertySources().addBefore(name, propertySource);
			}
		});
		return report;
	}

	private PropertySource<?> mapPropertiesWithReplacement(
			DeprecatedPropertiesReport report, String name,
			List<DeprecatedProperty> properties) {
		List<DeprecatedProperty> renamed = new ArrayList<>();
		List<DeprecatedProperty> unsupported = new ArrayList<>();
		properties.forEach((property) -> {
			(isRenamed(property) ? renamed : unsupported).add(property);
		});
		report.add(name, renamed, unsupported);
		if (renamed.isEmpty()) {
			return null;
		}
		String target = "migrate-" + name;
		Map<String, OriginTrackedValue> content = new LinkedHashMap<>();
		for (DeprecatedProperty candidate : renamed) {
			OriginTrackedValue value = OriginTrackedValue.of(
					candidate.getProperty().getValue(),
					candidate.getProperty().getOrigin());
			content.put(candidate.getMetadata().getDeprecation().getReplacement(), value);
		}
		return new OriginTrackedMapPropertySource(target, content);
	}

	private boolean isRenamed(DeprecatedProperty property) {
		ConfigurationMetadataProperty metadata = property.getMetadata();
		String replacementId = metadata.getDeprecation().getReplacement();
		if (StringUtils.hasText(replacementId)) {
			ConfigurationMetadataProperty replacement = this.allProperties
					.get(replacementId);
			if (replacement != null) {
				return replacement.getType().equals(metadata.getType());
			}
			replacement = getMapProperty(replacementId);
			if (replacement != null) {
				return replacement.getType().startsWith("java.util.Map")
						&& replacement.getType().endsWith(metadata.getType() + ">");
			}
		}
		return false;
	}

	private ConfigurationMetadataProperty getMapProperty(String fullId) {
		int i = fullId.lastIndexOf('.');
		if (i != -1) {
			return this.allProperties.get(fullId.substring(0, i));
		}
		return null;
	}

	private Map<String, List<DeprecatedProperty>> getMatchingProperties(
			Predicate<ConfigurationMetadataProperty> filter) {
		MultiValueMap<String, DeprecatedProperty> result = new LinkedMultiValueMap<>();
		List<ConfigurationMetadataProperty> candidates = this.allProperties.values()
				.stream().filter(filter).collect(Collectors.toList());
		getPropertySourcesAsMap().forEach((name, source) -> {
			candidates.forEach(metadata -> {
				ConfigurationProperty configurationProperty = source
						.getConfigurationProperty(
								ConfigurationPropertyName.of(metadata.getId()));
				if (configurationProperty != null) {
					result.add(name,
							new DeprecatedProperty(metadata, configurationProperty));
				}
			});
		});
		return result;
	}

	private Predicate<ConfigurationMetadataProperty> deprecatedFilter() {
		return (property) -> property.getDeprecation() != null
				&& property.getDeprecation().getLevel() == Deprecation.Level.ERROR;
	}

	private Map<String, ConfigurationPropertySource> getPropertySourcesAsMap() {
		Map<String, ConfigurationPropertySource> map = new LinkedHashMap<>();
		ConfigurationPropertySources.get(this.environment);
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
