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

package org.springframework.boot.build.context.properties;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.api.tasks.SourceTask;
import org.gradle.api.tasks.TaskAction;

/**
 * {@link SourceTask} that checks {@code spring-configuration-metadata.json} files.
 *
 * @author Andy Wilkinson
 */
public class CheckSpringConfigurationMetadata extends DefaultTask {

	private List<String> exclusions = new ArrayList<>();

	private final RegularFileProperty reportLocation;

	private final RegularFileProperty metadataLocation;

	/**
	 * Constructor for the CheckSpringConfigurationMetadata class. Initializes the
	 * metadataLocation and reportLocation properties.
	 */
	public CheckSpringConfigurationMetadata() {
		this.metadataLocation = getProject().getObjects().fileProperty();
		this.reportLocation = getProject().getObjects().fileProperty();
	}

	/**
	 * Returns the location of the report file.
	 * @return the location of the report file
	 */
	@OutputFile
	public RegularFileProperty getReportLocation() {
		return this.reportLocation;
	}

	/**
	 * Retrieves the metadata location of the input file.
	 * @return The metadata location of the input file.
	 */
	@InputFile
	@PathSensitive(PathSensitivity.RELATIVE)
	public RegularFileProperty getMetadataLocation() {
		return this.metadataLocation;
	}

	/**
	 * Sets the list of exclusions for the CheckSpringConfigurationMetadata class.
	 * @param exclusions the list of exclusions to be set
	 */
	public void setExclusions(List<String> exclusions) {
		this.exclusions = exclusions;
	}

	/**
	 * Retrieves the list of exclusions.
	 * @return the list of exclusions
	 */
	@Input
	public List<String> getExclusions() {
		return this.exclusions;
	}

	/**
	 * Checks the Spring configuration metadata.
	 * @throws JsonParseException if there is an error parsing the JSON.
	 * @throws IOException if there is an error reading or writing the file.
	 * @throws GradleException if problems are found in the Spring configuration metadata.
	 * The details can be found in the generated report file.
	 */
	@TaskAction
	void check() throws JsonParseException, IOException {
		Report report = createReport();
		File reportFile = getReportLocation().get().getAsFile();
		Files.write(reportFile.toPath(), report, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
		if (report.hasProblems()) {
			throw new GradleException(
					"Problems found in Spring configuration metadata. See " + reportFile + " for details.");
		}
	}

	/**
	 * Creates a report based on the metadata file.
	 * @return The generated report.
	 * @throws IOException If an I/O error occurs while reading the metadata file.
	 * @throws JsonParseException If the metadata file is not a valid JSON.
	 * @throws JsonMappingException If there is an error mapping the JSON to the Report
	 * object.
	 */
	@SuppressWarnings("unchecked")
	private Report createReport() throws IOException, JsonParseException, JsonMappingException {
		ObjectMapper objectMapper = new ObjectMapper();
		File file = this.metadataLocation.get().getAsFile();
		Report report = new Report(getProject().getProjectDir().toPath().relativize(file.toPath()));
		Map<String, Object> json = objectMapper.readValue(file, Map.class);
		List<Map<String, Object>> properties = (List<Map<String, Object>>) json.get("properties");
		for (Map<String, Object> property : properties) {
			String name = (String) property.get("name");
			if (!isDeprecated(property) && !isDescribed(property) && !isExcluded(name)) {
				report.propertiesWithNoDescription.add(name);
			}
		}
		return report;
	}

	/**
	 * Checks if a given property name is excluded based on the list of exclusions.
	 * @param propertyName the name of the property to check
	 * @return true if the property is excluded, false otherwise
	 */
	private boolean isExcluded(String propertyName) {
		for (String exclusion : this.exclusions) {
			if (propertyName.equals(exclusion)) {
				return true;
			}
			if (exclusion.endsWith(".*")) {
				if (propertyName.startsWith(exclusion.substring(0, exclusion.length() - 2))) {
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * Checks if a property is deprecated.
	 * @param property the property to check
	 * @return true if the property is deprecated, false otherwise
	 */
	@SuppressWarnings("unchecked")
	private boolean isDeprecated(Map<String, Object> property) {
		return (Map<String, Object>) property.get("deprecation") != null;
	}

	/**
	 * Checks if the given property is described.
	 * @param property the property to check
	 * @return true if the property is described, false otherwise
	 */
	private boolean isDescribed(Map<String, Object> property) {
		return property.get("description") != null;
	}

	/**
	 * Report class.
	 */
	private static final class Report implements Iterable<String> {

		private final List<String> propertiesWithNoDescription = new ArrayList<>();

		private final Path source;

		/**
		 * Constructs a new Report object with the specified source path.
		 * @param source the path to the source file
		 */
		private Report(Path source) {
			this.source = source;
		}

		/**
		 * Checks if the report has any properties with no description.
		 * @return true if the report has properties with no description, false otherwise.
		 */
		private boolean hasProblems() {
			return !this.propertiesWithNoDescription.isEmpty();
		}

		/**
		 * Returns an iterator over the lines of the report.
		 * @return an iterator over the lines of the report
		 */
		@Override
		public Iterator<String> iterator() {
			List<String> lines = new ArrayList<>();
			lines.add(this.source.toString());
			lines.add("");
			if (this.propertiesWithNoDescription.isEmpty()) {
				lines.add("No problems found.");
			}
			else {
				lines.add("The following properties have no description:");
				lines.add("");
				lines.addAll(this.propertiesWithNoDescription.stream().map((line) -> "\t" + line).toList());
			}
			lines.add("");
			return lines.iterator();

		}

	}

}
