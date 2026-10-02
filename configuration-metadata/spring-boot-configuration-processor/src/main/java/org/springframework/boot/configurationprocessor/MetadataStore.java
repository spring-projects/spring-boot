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

package org.springframework.boot.configurationprocessor;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.function.Function;

import javax.tools.Diagnostic;

import org.springframework.boot.configurationprocessor.metadata.ConfigurationMetadata;
import org.springframework.boot.configurationprocessor.metadata.InvalidConfigurationMetadataException;
import org.springframework.boot.configurationprocessor.metadata.JsonMarshaller;
import org.springframework.boot.configurationprocessor.model.TypeDeclaration;

/**
 * A {@code MetadataStore} is responsible for the storage of metadata on the filesystem.
 *
 * @author Andy Wilkinson
 * @author Scott Frederick
 */
class MetadataStore {

	static final String METADATA_PATH = "META-INF/spring-configuration-metadata.json";

	static final Function<TypeDeclaration, String> SOURCE_METADATA_PATH = (
			type) -> "META-INF/spring/configuration-metadata/%s.json".formatted(type.getQualifiedName());

	private static final String ADDITIONAL_METADATA_PATH = "META-INF/additional-spring-configuration-metadata.json";

	static final Function<TypeDeclaration, String> ADDITIONAL_SOURCE_METADATA_PATH = (
			type) -> "META-INF/spring/configuration-metadata/additional/%s.json".formatted(type.getQualifiedName());

	private final ProcessingContext context;

	MetadataStore(ProcessingContext context) {
		this.context = context;
	}

	/**
	 * Read the existing {@link ConfigurationMetadata} of the current module or
	 * {@code null} if it is not available yet.
	 * @return the metadata or {@code null} if none is present
	 */
	ConfigurationMetadata readMetadata() {
		return readMetadata(METADATA_PATH);
	}

	/**
	 * Read the existing {@link ConfigurationMetadata} for the specified type or
	 * {@code null} if it is not available yet.
	 * @param typeElement the type to read metadata for
	 * @return the metadata for the given type or {@code null}
	 */
	ConfigurationMetadata readMetadata(TypeDeclaration typeElement) {
		return readMetadata(SOURCE_METADATA_PATH.apply(typeElement));
	}

	private ConfigurationMetadata readMetadata(String location) {
		try {
			return readMetadata(location, this.context.openResource(location));
		}
		catch (IOException ex) {
			return null;
		}
	}

	/**
	 * Write the module {@link ConfigurationMetadata} to the filesystem.
	 * @param metadata the metadata to write
	 * @throws IOException when the write fails
	 */
	void writeMetadata(ConfigurationMetadata metadata) throws IOException {
		writeMetadata(metadata, METADATA_PATH);
	}

	/**
	 * Write the {@link ConfigurationMetadata} for the {@link TypeDeclaration} to the
	 * filesystem.
	 * @param metadata the metadata to write
	 * @param typeElement the type to write metadata for
	 * @throws IOException when the write fails
	 */
	void writeMetadata(ConfigurationMetadata metadata, TypeDeclaration typeElement) throws IOException {
		writeMetadata(metadata, SOURCE_METADATA_PATH.apply(typeElement));
	}

	private void writeMetadata(ConfigurationMetadata metadata, String location) throws IOException {
		if (!metadata.getItems().isEmpty()) {
			try (OutputStream outputStream = this.context.createResource(location)) {
				new JsonMarshaller().write(metadata, outputStream);
			}
		}
	}

	/**
	 * Read additional {@link ConfigurationMetadata} for the current module or
	 * {@code null}.
	 * @return additional metadata or {@code null} if none is present
	 */
	ConfigurationMetadata readAdditionalMetadata() {
		return readAdditionalMetadata(ADDITIONAL_METADATA_PATH);
	}

	/**
	 * Read additional {@link ConfigurationMetadata} for the {@link TypeDeclaration} or
	 * {@code null}.
	 * @param typeElement the type to get additional metadata for
	 * @return additional metadata for the given type or {@code null} if none is present
	 */
	ConfigurationMetadata readAdditionalMetadata(TypeDeclaration typeElement) {
		return readAdditionalMetadata(ADDITIONAL_SOURCE_METADATA_PATH.apply(typeElement));
	}

	private ConfigurationMetadata readAdditionalMetadata(String location) {
		try {
			InputStream in = this.context.openAdditionalMetadata(location);
			return readMetadata(location, in);
		}
		catch (IOException ex) {
			return null;
		}
	}

	private ConfigurationMetadata readMetadata(String location, InputStream in) {
		try (in) {
			return new JsonMarshaller().read(in);
		}
		catch (IOException ex) {
			return null;
		}
		catch (Exception ex) {
			throw new InvalidConfigurationMetadataException(
					"Invalid additional meta-data in '" + location + "': " + ex.getMessage(), Diagnostic.Kind.ERROR);
		}
	}

}
