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

package org.springframework.boot.configurationprocessor;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.annotation.processing.ProcessingEnvironment;
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
 * @since 1.2.2
 */
public class MetadataStore {

	static final String METADATA_PATH = "META-INF/spring-configuration-metadata.json";

	private static final String ADDITIONAL_METADATA_PATH = "META-INF/additional-spring-configuration-metadata.json";

	private static final String RESOURCES_DIRECTORY = "resources";

	private static final String CLASSES_DIRECTORY = "classes";

	private final ProcessingEnvironment environment;

	/**
     * Constructs a new MetadataStore object with the specified ProcessingEnvironment.
     * 
     * @param environment the ProcessingEnvironment used for processing metadata
     */
    public MetadataStore(ProcessingEnvironment environment) {
		this.environment = environment;
	}

	/**
     * Reads the metadata from the metadata resource.
     * 
     * @return the configuration metadata
     * @throws IOException if an I/O error occurs while reading the metadata
     */
    public ConfigurationMetadata readMetadata() {
		try {
			return readMetadata(getMetadataResource().openInputStream());
		}
		catch (IOException ex) {
			return null;
		}
	}

	/**
     * Writes the given ConfigurationMetadata to a metadata resource.
     * 
     * @param metadata the ConfigurationMetadata to write
     * @throws IOException if an I/O error occurs while writing the metadata
     */
    public void writeMetadata(ConfigurationMetadata metadata) throws IOException {
		if (!metadata.getItems().isEmpty()) {
			try (OutputStream outputStream = createMetadataResource().openOutputStream()) {
				new JsonMarshaller().write(metadata, outputStream);
			}
		}
	}

	/**
     * Reads additional metadata from the additional metadata stream.
     * 
     * @return the configuration metadata read from the additional metadata stream
     * @throws IOException if an I/O error occurs while reading the additional metadata
     */
    public ConfigurationMetadata readAdditionalMetadata() throws IOException {
		return readMetadata(getAdditionalMetadataStream());
	}

	/**
     * Reads the metadata from the given input stream.
     * 
     * @param in the input stream to read the metadata from
     * @return the configuration metadata read from the input stream
     * @throws InvalidConfigurationMetadataException if the additional meta-data is invalid
     * @throws IOException if an I/O error occurs while reading the input stream
     */
    private ConfigurationMetadata readMetadata(InputStream in) {
		try (in) {
			return new JsonMarshaller().read(in);
		}
		catch (IOException ex) {
			return null;
		}
		catch (Exception ex) {
			throw new InvalidConfigurationMetadataException(
					"Invalid additional meta-data in '" + METADATA_PATH + "': " + ex.getMessage(),
					Diagnostic.Kind.ERROR);
		}
	}

	/**
     * Retrieves the metadata resource file object.
     * 
     * @return the metadata resource file object
     * @throws IOException if an I/O error occurs while retrieving the resource
     */
    private FileObject getMetadataResource() throws IOException {
		return this.environment.getFiler().getResource(StandardLocation.CLASS_OUTPUT, "", METADATA_PATH);
	}

	/**
     * Creates a metadata resource file.
     * 
     * @return the created metadata resource file
     * @throws IOException if an I/O error occurs while creating the resource file
     */
    private FileObject createMetadataResource() throws IOException {
		return this.environment.getFiler().createResource(StandardLocation.CLASS_OUTPUT, "", METADATA_PATH);
	}

	/**
     * Retrieves the additional metadata stream.
     * 
     * @return the input stream containing the additional metadata
     * @throws IOException if an I/O error occurs while retrieving the stream
     */
    private InputStream getAdditionalMetadataStream() throws IOException {
		// Most build systems will have copied the file to the class output location
		FileObject fileObject = this.environment.getFiler()
			.getResource(StandardLocation.CLASS_OUTPUT, "", ADDITIONAL_METADATA_PATH);
		InputStream inputStream = getMetadataStream(fileObject);
		if (inputStream != null) {
			return inputStream;
		}
		try {
			File file = locateAdditionalMetadataFile(new File(fileObject.toUri()));
			return (file.exists() ? new FileInputStream(file) : fileObject.toUri().toURL().openStream());
		}
		catch (Exception ex) {
			throw new FileNotFoundException();
		}
	}

	/**
     * Retrieves the metadata stream for the given file object.
     * 
     * @param fileObject the file object for which to retrieve the metadata stream
     * @return the metadata stream as an InputStream, or null if an IOException occurs
     */
    private InputStream getMetadataStream(FileObject fileObject) {
		try {
			return fileObject.openInputStream();
		}
		catch (IOException ex) {
			return null;
		}
	}

	/**
     * Locates the additional metadata file based on the standard location.
     * 
     * @param standardLocation the standard location of the metadata file
     * @return the additional metadata file if found, otherwise the standard location file
     * @throws IOException if an I/O error occurs while locating the file
     */
    File locateAdditionalMetadataFile(File standardLocation) throws IOException {
		if (standardLocation.exists()) {
			return standardLocation;
		}
		String locations = this.environment.getOptions()
			.get(ConfigurationMetadataAnnotationProcessor.ADDITIONAL_METADATA_LOCATIONS_OPTION);
		if (locations != null) {
			for (String location : locations.split(",")) {
				File candidate = new File(location, ADDITIONAL_METADATA_PATH);
				if (candidate.isFile()) {
					return candidate;
				}
			}
		}
		return new File(locateGradleResourcesDirectory(standardLocation), ADDITIONAL_METADATA_PATH);
	}

	/**
     * Locates the Gradle resources directory based on the standard additional metadata location.
     * 
     * @param standardAdditionalMetadataLocation the standard additional metadata location
     * @return the Gradle resources directory
     * @throws FileNotFoundException if the Gradle resources directory cannot be found
     */
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

}
