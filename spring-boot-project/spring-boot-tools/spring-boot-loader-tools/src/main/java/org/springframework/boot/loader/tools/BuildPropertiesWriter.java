/*
 * Copyright 2012-2021 the original author or authors.
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

package org.springframework.boot.loader.tools;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Properties;

import org.springframework.core.CollectionFactory;
import org.springframework.util.StringUtils;

/**
 * A {@code BuildPropertiesWriter} writes the {@code build-info.properties} for
 * consumption by the Actuator.
 *
 * @author Andy Wilkinson
 * @author Stephane Nicoll
 * @author Vedran Pavic
 * @since 1.0.0
 */
public final class BuildPropertiesWriter {

	private final File outputFile;

	/**
	 * Creates a new {@code BuildPropertiesWriter} that will write to the given
	 * {@code outputFile}.
	 * @param outputFile the output file
	 */
	public BuildPropertiesWriter(File outputFile) {
		this.outputFile = outputFile;
	}

	/**
	 * Writes the build properties to a file.
	 * @param projectDetails the details of the project
	 * @throws IOException if an I/O error occurs
	 */
	public void writeBuildProperties(ProjectDetails projectDetails) throws IOException {
		Properties properties = createBuildInfo(projectDetails);
		createFileIfNecessary(this.outputFile);
		try (FileOutputStream outputStream = new FileOutputStream(this.outputFile)) {
			properties.store(outputStream, "Properties");
		}
	}

	/**
	 * Creates a file if it does not already exist.
	 * @param file the file to be created
	 * @throws IOException if an I/O error occurs while creating the file
	 * @throws IllegalStateException if the parent directory cannot be created or if the
	 * target file cannot be created
	 */
	private void createFileIfNecessary(File file) throws IOException {
		if (file.exists()) {
			return;
		}
		File parent = file.getParentFile();
		if (!parent.isDirectory() && !parent.mkdirs()) {
			throw new IllegalStateException(
					"Cannot create parent directory for '" + this.outputFile.getAbsolutePath() + "'");
		}
		if (!file.createNewFile()) {
			throw new IllegalStateException("Cannot create target file '" + this.outputFile.getAbsolutePath() + "'");
		}
	}

	/**
	 * Creates a Properties object containing build information for the given project.
	 * @param project the ProjectDetails object representing the project
	 * @return a Properties object containing build information
	 */
	protected Properties createBuildInfo(ProjectDetails project) {
		Properties properties = CollectionFactory.createSortedProperties(true);
		addIfHasValue(properties, "build.group", project.getGroup());
		addIfHasValue(properties, "build.artifact", project.getArtifact());
		addIfHasValue(properties, "build.name", project.getName());
		addIfHasValue(properties, "build.version", project.getVersion());
		if (project.getTime() != null) {
			properties.put("build.time", DateTimeFormatter.ISO_INSTANT.format(project.getTime()));
		}
		if (project.getAdditionalProperties() != null) {
			project.getAdditionalProperties().forEach((name, value) -> properties.put("build." + name, value));
		}
		return properties;
	}

	/**
	 * Adds the specified name-value pair to the given properties object if the value is
	 * not empty or null.
	 * @param properties the properties object to add the name-value pair to
	 * @param name the name of the property
	 * @param value the value of the property
	 */
	private void addIfHasValue(Properties properties, String name, String value) {
		if (StringUtils.hasText(value)) {
			properties.put(name, value);
		}
	}

	/**
	 * Build-system agnostic details of a project.
	 */
	public static final class ProjectDetails {

		private final String group;

		private final String artifact;

		private final String name;

		private final String version;

		private final Instant time;

		private final Map<String, String> additionalProperties;

		/**
		 * Constructs a new ProjectDetails object with the specified group, artifact,
		 * version, name, time, and additional properties.
		 * @param group the group of the project
		 * @param artifact the artifact of the project
		 * @param version the version of the project
		 * @param name the name of the project
		 * @param time the time of the project
		 * @param additionalProperties the additional properties of the project
		 * @throws IllegalArgumentException if the additional properties are invalid
		 */
		public ProjectDetails(String group, String artifact, String version, String name, Instant time,
				Map<String, String> additionalProperties) {
			this.group = group;
			this.artifact = artifact;
			this.name = name;
			this.version = version;
			this.time = time;
			validateAdditionalProperties(additionalProperties);
			this.additionalProperties = additionalProperties;
		}

		/**
		 * Validates the additional properties of a project.
		 * @param additionalProperties the map of additional properties to be validated
		 * @throws NullAdditionalPropertyValueException if any additional property value
		 * is null
		 */
		private static void validateAdditionalProperties(Map<String, String> additionalProperties) {
			if (additionalProperties != null) {
				additionalProperties.forEach((name, value) -> {
					if (value == null) {
						throw new NullAdditionalPropertyValueException(name);
					}
				});
			}
		}

		/**
		 * Returns the group of the project.
		 * @return the group of the project
		 */
		public String getGroup() {
			return this.group;
		}

		/**
		 * Returns the artifact of the ProjectDetails.
		 * @return the artifact of the ProjectDetails
		 */
		public String getArtifact() {
			return this.artifact;
		}

		/**
		 * Returns the name of the ProjectDetails object.
		 * @return the name of the ProjectDetails object
		 */
		public String getName() {
			return this.name;
		}

		/**
		 * Returns the version of the project.
		 * @return the version of the project
		 */
		public String getVersion() {
			return this.version;
		}

		/**
		 * Returns the current time.
		 * @return the current time as an Instant object
		 */
		public Instant getTime() {
			return this.time;
		}

		/**
		 * Returns the additional properties of the ProjectDetails.
		 * @return the additional properties as a Map<String, String>
		 */
		public Map<String, String> getAdditionalProperties() {
			return this.additionalProperties;
		}

	}

	/**
	 * Exception thrown when an additional property with a null value is encountered.
	 */
	public static class NullAdditionalPropertyValueException extends IllegalArgumentException {

		/**
		 * Constructs a new NullAdditionalPropertyValueException with the specified
		 * property name.
		 * @param name the name of the additional property
		 */
		public NullAdditionalPropertyValueException(String name) {
			super("Additional property '" + name + "' is illegal as its value is null");
		}

	}

}
