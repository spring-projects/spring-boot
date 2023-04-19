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

	public CheckSpringConfigurationMetadata() {
		this.metadataLocation = getProject().getObjects().fileProperty();
		this.reportLocation = getProject().getObjects().fileProperty();
	}

	@OutputFile
	public RegularFileProperty getReportLocation() {
		return this.reportLocation;
	}

	@InputFile
	@PathSensitive(PathSensitivity.RELATIVE)
	public RegularFileProperty getMetadataLocation() {
		return this.metadataLocation;
	}

	public void setExclusions(List<String> exclusions) {
		this.exclusions = exclusions;
	}

	@Input
	public List<String> getExclusions() {
		return this.exclusions;
	}

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

	@SuppressWarnings("unchecked")
	private boolean isDeprecated(Map<String, Object> property) {
		return (Map<String, Object>) property.get("deprecation") != null;
	}

	private boolean isDescribed(Map<String, Object> property) {
		return property.get("description") != null;
	}

	private static final class Report implements Iterable<String> {

		private final List<String> propertiesWithNoDescription = new ArrayList<>();

		private final Path source;

		private Report(Path source) {
			this.source = source;
		}

		private boolean hasProblems() {
			return !this.propertiesWithNoDescription.isEmpty();
		}

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
