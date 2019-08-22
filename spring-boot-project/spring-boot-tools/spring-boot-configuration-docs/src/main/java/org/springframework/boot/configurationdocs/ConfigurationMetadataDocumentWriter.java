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

package org.springframework.boot.configurationdocs;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.boot.configurationmetadata.ConfigurationMetadataProperty;
import org.springframework.boot.configurationmetadata.ConfigurationMetadataRepositoryJsonBuilder;

/**
 * Write Asciidoc documents with configuration properties listings.
 *
 * @author Brian Clozel
 * @since 2.0.0
 */
public class ConfigurationMetadataDocumentWriter {

	public void writeDocument(Path outputDirectory, DocumentOptions options, InputStream... metadata)
			throws IOException {
		assertValidOutputDirectory(outputDirectory);
		if (!Files.exists(outputDirectory)) {
			Files.createDirectory(outputDirectory);
		}
		assertMetadata(metadata);
		List<ConfigurationTable> tables = createConfigTables(getMetadataProperties(metadata), options);
		for (ConfigurationTable table : tables) {
			writeConfigurationTable(table, outputDirectory);
		}
	}

	private void assertValidOutputDirectory(Path outputDirPath) {
		if (outputDirPath == null) {
			throw new IllegalArgumentException("output path should not be null");
		}
		if (Files.exists(outputDirPath) && !Files.isDirectory(outputDirPath)) {
			throw new IllegalArgumentException("output path already exists and is not a directory");
		}
	}

	private void assertMetadata(InputStream... metadata) {
		if (metadata == null || metadata.length < 1) {
			throw new IllegalArgumentException("missing input metadata");
		}
	}

	private Map<String, ConfigurationMetadataProperty> getMetadataProperties(InputStream... metadata)
			throws IOException {
		ConfigurationMetadataRepositoryJsonBuilder builder = ConfigurationMetadataRepositoryJsonBuilder
				.create(metadata);
		return builder.build().getAllProperties();
	}

	private List<ConfigurationTable> createConfigTables(Map<String, ConfigurationMetadataProperty> metadataProperties,
			DocumentOptions options) {
		List<ConfigurationTable> tables = new ArrayList<>();
		List<String> unmappedKeys = metadataProperties.values().stream().filter((property) -> !property.isDeprecated())
				.map(ConfigurationMetadataProperty::getId).collect(Collectors.toList());
		Map<String, CompoundConfigurationTableEntry> overrides = getOverrides(metadataProperties, unmappedKeys,
				options);
		options.getMetadataSections().forEach((id, keyPrefixes) -> tables
				.add(createConfigTable(metadataProperties, unmappedKeys, overrides, id, keyPrefixes)));
		if (!unmappedKeys.isEmpty()) {
			throw new IllegalStateException(
					"The following keys were not written to the documentation: " + String.join(", ", unmappedKeys));
		}
		if (!overrides.isEmpty()) {
			throw new IllegalStateException("The following keys  were not written to the documentation: "
					+ String.join(", ", overrides.keySet()));
		}
		return tables;
	}

	private Map<String, CompoundConfigurationTableEntry> getOverrides(
			Map<String, ConfigurationMetadataProperty> metadataProperties, List<String> unmappedKeys,
			DocumentOptions options) {
		Map<String, CompoundConfigurationTableEntry> overrides = new HashMap<>();
		options.getOverrides().forEach((keyPrefix, description) -> {
			CompoundConfigurationTableEntry entry = new CompoundConfigurationTableEntry(keyPrefix, description);
			List<String> matchingKeys = unmappedKeys.stream().filter((key) -> key.startsWith(keyPrefix))
					.collect(Collectors.toList());
			for (String matchingKey : matchingKeys) {
				entry.addConfigurationKeys(metadataProperties.get(matchingKey));
			}
			overrides.put(keyPrefix, entry);
			unmappedKeys.removeAll(matchingKeys);
		});
		return overrides;
	}

	private ConfigurationTable createConfigTable(Map<String, ConfigurationMetadataProperty> metadataProperties,
			List<String> unmappedKeys, Map<String, CompoundConfigurationTableEntry> overrides, String id,
			List<String> keyPrefixes) {
		ConfigurationTable table = new ConfigurationTable(id);
		for (String keyPrefix : keyPrefixes) {
			List<String> matchingOverrides = overrides.keySet().stream()
					.filter((overrideKey) -> overrideKey.startsWith(keyPrefix)).collect(Collectors.toList());
			matchingOverrides.forEach((match) -> table.addEntry(overrides.remove(match)));
		}
		List<String> matchingKeys = unmappedKeys.stream()
				.filter((key) -> keyPrefixes.stream().anyMatch(key::startsWith)).collect(Collectors.toList());
		for (String matchingKey : matchingKeys) {
			ConfigurationMetadataProperty property = metadataProperties.get(matchingKey);
			table.addEntry(new SingleConfigurationTableEntry(property));
		}
		unmappedKeys.removeAll(matchingKeys);
		return table;
	}

	private void writeConfigurationTable(ConfigurationTable table, Path outputDirectory) throws IOException {
		Path outputFilePath = outputDirectory.resolve(table.getId() + ".adoc");
		Files.deleteIfExists(outputFilePath);
		Files.createFile(outputFilePath);
		try (OutputStream outputStream = Files.newOutputStream(outputFilePath)) {
			outputStream.write(table.toAsciidocTable().getBytes(StandardCharsets.UTF_8));
		}
	}

}
