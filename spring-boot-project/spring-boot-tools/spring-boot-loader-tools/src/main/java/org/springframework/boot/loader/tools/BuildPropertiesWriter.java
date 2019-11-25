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

package org.springframework.boot.loader.tools;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Properties;

import org.springframework.core.CollectionFactory;

/**
 * A {@code BuildPropertiesWriter} writes the {@code build-info.properties} for
 * consumption by the Actuator.
 *
 * @author Andy Wilkinson
 * @author Stephane Nicoll
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

	public void writeBuildProperties(ProjectDetails projectDetails) throws IOException {
		Properties properties = createBuildInfo(projectDetails);
		createFileIfNecessary(this.outputFile);
		try (FileOutputStream outputStream = new FileOutputStream(this.outputFile)) {
			properties.store(outputStream, "Properties");
		}
	}

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

	protected Properties createBuildInfo(ProjectDetails project) {
		Properties properties = CollectionFactory.createSortedProperties(true);
		properties.put("build.group", project.getGroup());
		properties.put("build.artifact", project.getArtifact());
		properties.put("build.name", project.getName());
		properties.put("build.version", project.getVersion());
		if (project.getTime() != null) {
			properties.put("build.time", DateTimeFormatter.ISO_INSTANT.format(project.getTime()));
		}
		if (project.getAdditionalProperties() != null) {
			project.getAdditionalProperties().forEach((name, value) -> properties.put("build." + name, value));
		}
		return properties;
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

		private static void validateAdditionalProperties(Map<String, String> additionalProperties) {
			if (additionalProperties != null) {
				additionalProperties.forEach((name, value) -> {
					if (value == null) {
						throw new NullAdditionalPropertyValueException(name);
					}
				});
			}
		}

		public String getGroup() {
			return this.group;
		}

		public String getArtifact() {
			return this.artifact;
		}

		public String getName() {
			return this.name;
		}

		public String getVersion() {
			return this.version;
		}

		public Instant getTime() {
			return this.time;
		}

		public Map<String, String> getAdditionalProperties() {
			return this.additionalProperties;
		}

	}

	/**
	 * Exception thrown when an additional property with a null value is encountered.
	 */
	public static class NullAdditionalPropertyValueException extends IllegalArgumentException {

		public NullAdditionalPropertyValueException(String name) {
			super("Additional property '" + name + "' is illegal as its value is null");
		}

	}

}
