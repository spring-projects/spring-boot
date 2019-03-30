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
import org.springframework.boot.configurationmetadata.ConfigurationMetadataRepository;
import org.springframework.boot.configurationmetadata.ConfigurationMetadataRepositoryJsonBuilder;

/**
 * Write Asciidoc documents with configuration properties listings.
 *
 * @author Brian Clozel
 */
public class ConfigurationMetadataDocumentWriter {

	public void writeDocument(Path outputDirPath, DocumentOptions options,
			InputStream... metadataInput) throws IOException {
		if (outputDirPath == null) {
			throw new IllegalArgumentException("output path should not be null");
		}
		if (Files.exists(outputDirPath) && !Files.isDirectory(outputDirPath)) {
			throw new IllegalArgumentException(
					"output path already exists and is not a directory");
		}
		else if (!Files.exists(outputDirPath)) {
			Files.createDirectory(outputDirPath);
		}
		if (metadataInput == null || metadataInput.length < 1) {
			throw new IllegalArgumentException("missing input metadata");
		}

		ConfigurationMetadataRepository configRepository = ConfigurationMetadataRepositoryJsonBuilder
				.create(metadataInput).build();
		Map<String, ConfigurationMetadataProperty> allProperties = configRepository
				.getAllProperties();

		List<ConfigurationTable> tables = createConfigTables(allProperties, options);

		for (ConfigurationTable table : tables) {
			Path outputFilePath = outputDirPath.resolve(table.getId() + ".adoc");
			Files.deleteIfExists(outputFilePath);
			Files.createFile(outputFilePath);
			try (OutputStream outputStream = Files.newOutputStream(outputFilePath)) {
				outputStream
						.write(table.toAsciidocTable().getBytes(StandardCharsets.UTF_8));
			}
		}
	}

	private List<ConfigurationTable> createConfigTables(
			Map<String, ConfigurationMetadataProperty> allProperties,
			DocumentOptions options) {

		final List<ConfigurationTable> tables = new ArrayList<>();
		final List<String> unmappedKeys = allProperties.values().stream()
				.filter((prop) -> !prop.isDeprecated()).map((prop) -> prop.getId())
				.collect(Collectors.toList());

		final Map<String, CompoundKeyEntry> overrides = getOverrides(allProperties,
				unmappedKeys, options);

		options.getMetadataSections().forEach((id, keyPrefixes) -> {
			ConfigurationTable table = new ConfigurationTable(id);
			tables.add(table);
			for (String keyPrefix : keyPrefixes) {
				List<String> matchingOverrides = overrides.keySet().stream()
						.filter((overrideKey) -> overrideKey.startsWith(keyPrefix))
						.collect(Collectors.toList());
				matchingOverrides
						.forEach((match) -> table.addEntry(overrides.remove(match)));
			}
			List<String> matchingKeys = unmappedKeys.stream()
					.filter((key) -> keyPrefixes.stream().anyMatch(key::startsWith))
					.collect(Collectors.toList());
			for (String matchingKey : matchingKeys) {
				ConfigurationMetadataProperty property = allProperties.get(matchingKey);
				table.addEntry(new SingleKeyEntry(property));

			}
			unmappedKeys.removeAll(matchingKeys);
		});

		if (!unmappedKeys.isEmpty()) {
			throw new IllegalStateException(
					"The following keys were not written to the documentation: "
							+ String.join(", ", unmappedKeys));
		}
		if (!overrides.isEmpty()) {
			throw new IllegalStateException(
					"The following keys  were not written to the documentation: "
							+ String.join(", ", overrides.keySet()));
		}

		return tables;
	}

	private Map<String, CompoundKeyEntry> getOverrides(
			Map<String, ConfigurationMetadataProperty> allProperties,
			List<String> unmappedKeys, DocumentOptions options) {
		final Map<String, CompoundKeyEntry> overrides = new HashMap<>();

		options.getOverrides().forEach((keyPrefix, description) -> {
			final CompoundKeyEntry entry = new CompoundKeyEntry(keyPrefix, description);
			List<String> matchingKeys = unmappedKeys.stream()
					.filter((key) -> key.startsWith(keyPrefix))
					.collect(Collectors.toList());
			for (String matchingKey : matchingKeys) {
				entry.addConfigurationKeys(allProperties.get(matchingKey));
			}
			overrides.put(keyPrefix, entry);
			unmappedKeys.removeAll(matchingKeys);
		});
		return overrides;
	}

}
