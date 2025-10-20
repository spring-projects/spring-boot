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

import org.gradle.api.DefaultTask;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.api.tasks.SourceTask;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.VerificationException;
import tools.jackson.databind.json.JsonMapper;

/**
 * {@link SourceTask} that checks {@code spring-configuration-metadata.json} files.
 *
 * @author Andy Wilkinson
 */
public abstract class CheckSpringConfigurationMetadata extends DefaultTask {

	private final Path projectRoot;

	public CheckSpringConfigurationMetadata() {
		this.projectRoot = getProject().getProjectDir().toPath();
	}

	@OutputFile
	public abstract RegularFileProperty getReportLocation();

	@InputFile
	@PathSensitive(PathSensitivity.RELATIVE)
	public abstract RegularFileProperty getMetadataLocation();

	@Input
	public abstract ListProperty<String> getExclusions();

	@TaskAction
	void check() throws IOException {
		Report report = createReport();
		File reportFile = getReportLocation().get().getAsFile();
		Files.write(reportFile.toPath(), report, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
		if (report.hasProblems()) {
			throw new VerificationException(
					"Problems found in Spring configuration metadata. See " + reportFile + " for details.");
		}
	}

	@SuppressWarnings("unchecked")
	private Report createReport() {
		JsonMapper jsonMapper = new JsonMapper();
		File file = getMetadataLocation().get().getAsFile();
		Report report = new Report(this.projectRoot.relativize(file.toPath()));
		Map<String, Object> json = jsonMapper.readValue(file, Map.class);
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
		for (String exclusion : getExclusions().get()) {
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
