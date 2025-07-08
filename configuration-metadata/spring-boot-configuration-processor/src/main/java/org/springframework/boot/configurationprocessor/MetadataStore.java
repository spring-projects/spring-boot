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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.function.BiFunction;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;
import javax.tools.FileObject;
import javax.tools.StandardLocation;

import org.springframework.boot.configurationprocessor.metadata.ConfigurationMetadata;
import org.springframework.boot.configurationprocessor.metadata.InvalidConfigurationMetadataException;
import org.springframework.boot.configurationprocessor.metadata.JsonMarshaller;

/**
 * A {@code MetadataStore} is responsible for the storage of metadata on the filesystem.
 *
 * @author Andy Wilkinson
 * @author Scott Frederick
 */
class MetadataStore {

	static final String METADATA_PATH = "META-INF/spring-configuration-metadata.json";

	static final BiFunction<TypeElement, TypeUtils, String> SOURCE_METADATA_PATH = (type,
			typeUtils) -> "META-INF/spring/configuration-metadata/%s.json".formatted(typeUtils.getQualifiedName(type));

	private static final String ADDITIONAL_METADATA_PATH = "META-INF/additional-spring-configuration-metadata.json";

	static final BiFunction<TypeElement, TypeUtils, String> ADDITIONAL_SOURCE_METADATA_PATH = (type,
			typeUtils) -> "META-INF/spring/configuration-metadata/additional/%s.json"
				.formatted(typeUtils.getQualifiedName(type));

	private static final String RESOURCES_DIRECTORY = "resources";

	private static final String CLASSES_DIRECTORY = "classes";

	private final ProcessingEnvironment environment;

	private final TypeUtils typeUtils;

	MetadataStore(ProcessingEnvironment environment, TypeUtils typeUtils) {
		this.environment = environment;
		this.typeUtils = typeUtils;
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
	ConfigurationMetadata readMetadata(TypeElement typeElement) {
		return readMetadata(SOURCE_METADATA_PATH.apply(typeElement, this.typeUtils));
	}

	private ConfigurationMetadata readMetadata(String location) {
		try {
			return readMetadata(location, getMetadataResource(location).openInputStream());
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
		writeMetadata(metadata, () -> createMetadataResource(METADATA_PATH));
	}

	/**
	 * Write the {@link ConfigurationMetadata} for the {@link TypeElement} to the
	 * filesystem.
	 * @param metadata the metadata to write
	 * @param typeElement the type to write metadata for
	 * @throws IOException when the write fails
	 */
	void writeMetadata(ConfigurationMetadata metadata, TypeElement typeElement) throws IOException {
		writeMetadata(metadata, () -> createMetadataResource(SOURCE_METADATA_PATH.apply(typeElement, this.typeUtils)));
	}

	/**
	 * Write the metadata to the {@link FileObject} provided by the given supplier.
	 * @param metadata the metadata to provide
	 * @param fileObjectProvider a supplier for the {@link FileObject} to use
	 */
	private void writeMetadata(ConfigurationMetadata metadata, FileObjectSupplier fileObjectProvider)
			throws IOException {
		if (!metadata.getItems().isEmpty()) {
			try (OutputStream outputStream = fileObjectProvider.get().openOutputStream()) {
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
	 * Read additional {@link ConfigurationMetadata} for the {@link TypeElement} or
	 * {@code null}.
	 * @param typeElement the type to get additional metadata for
	 * @return additional metadata for the given type or {@code null} if none is present
	 */
	ConfigurationMetadata readAdditionalMetadata(TypeElement typeElement) {
		return readAdditionalMetadata(ADDITIONAL_SOURCE_METADATA_PATH.apply(typeElement, this.typeUtils));
	}

	private ConfigurationMetadata readAdditionalMetadata(String location) {
		try {
			InputStream in = getAdditionalMetadataStream(location);
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

	private FileObject getMetadataResource(String location) throws IOException {
		return this.environment.getFiler().getResource(StandardLocation.CLASS_OUTPUT, "", location);
	}

	private FileObject createMetadataResource(String location) throws IOException {
		return this.environment.getFiler().createResource(StandardLocation.CLASS_OUTPUT, "", location);
	}

	private InputStream getAdditionalMetadataStream(String additionalMetadataLocation) throws IOException {
		// Most build systems will have copied the file to the class output location
		FileObject fileObject = this.environment.getFiler()
			.getResource(StandardLocation.CLASS_OUTPUT, "", additionalMetadataLocation);
		InputStream inputStream = getMetadataStream(fileObject);
		if (inputStream != null) {
			return inputStream;
		}
		try {
			File file = locateAdditionalMetadataFile(new File(fileObject.toUri()), additionalMetadataLocation);
			return (file.exists() ? new FileInputStream(file) : fileObject.toUri().toURL().openStream());
		}
		catch (Exception ex) {
			throw new FileNotFoundException();
		}
	}

	private InputStream getMetadataStream(FileObject fileObject) {
		try {
			return fileObject.openInputStream();
		}
		catch (IOException ex) {
			return null;
		}
	}

	File locateAdditionalMetadataFile(File standardLocation, String additionalMetadataLocation) throws IOException {
		if (standardLocation.exists()) {
			return standardLocation;
		}
		String locations = this.environment.getOptions()
			.get(ConfigurationMetadataAnnotationProcessor.ADDITIONAL_METADATA_LOCATIONS_OPTION);
		if (locations != null) {
			for (String location : locations.split(",")) {
				File candidate = new File(location, additionalMetadataLocation);
				if (candidate.isFile()) {
					return candidate;
				}
			}
		}
		return new File(locateGradleResourcesDirectory(standardLocation), additionalMetadataLocation);
	}

	private File locateGradleResourcesDirectory(File standardAdditionalMetadataLocation) throws FileNotFoundException {
		String path = standardAdditionalMetadataLocation.getPath();
		int index = path.lastIndexOf(CLASSES_DIRECTORY);
		if (index < 0) {
			throw new FileNotFoundException();
		}
		String buildDirectoryPath = path.substring(0, index);
		File classOutputLocation = standardAdditionalMetadataLocation.getParentFile().getParentFile();
		return new File(buildDirectoryPath, RESOURCES_DIRECTORY + '/' + classOutputLocation.getName());
	}

	/**
	 * Internal callback that can throw an {@link IOException}.
	 */
	private interface FileObjectSupplier {

		FileObject get() throws IOException;

	}

}
