/*
 * Copyright 2025 the original author or authors.
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
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.gradle.api.DefaultTask;
import org.gradle.api.Task;
import org.gradle.api.file.FileCollection;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.VerificationException;

/**
 * {@link Task} that checks aggregated Spring configuration metadata.
 *
 * @author Andy Wilkinson
 */
public abstract class CheckAggregatedSpringConfigurationMetadata extends DefaultTask {

	private FileCollection configurationPropertyMetadata;

	@OutputFile
	public abstract RegularFileProperty getReportLocation();

	@InputFiles
	@PathSensitive(PathSensitivity.RELATIVE)
	public FileCollection getConfigurationPropertyMetadata() {
		return this.configurationPropertyMetadata;
	}

	public void setConfigurationPropertyMetadata(FileCollection configurationPropertyMetadata) {
		this.configurationPropertyMetadata = configurationPropertyMetadata;
	}

	@TaskAction
	void check() throws IOException {
		Report report = createReport();
		File reportFile = getReportLocation().get().getAsFile();
		Files.write(reportFile.toPath(), report, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
		if (report.hasProblems()) {
			throw new VerificationException(
					"Problems found in aggregated Spring configuration metadata. See " + reportFile + " for details.");
		}
	}

	private Report createReport() {
		ConfigurationProperties configurationProperties = ConfigurationProperties
			.fromFiles(this.configurationPropertyMetadata);
		Set<String> propertyNames = configurationProperties.stream()
			.map(ConfigurationProperty::getName)
			.collect(Collectors.toSet());
		List<ConfigurationProperty> missingReplacement = configurationProperties.stream()
			.filter(ConfigurationProperty::isDeprecated)
			.filter((deprecated) -> {
				String replacement = deprecated.getDeprecation().replacement();
				return replacement != null && !propertyNames.contains(replacement);
			})
			.toList();
		return new Report(missingReplacement);
	}

	private static final class Report implements Iterable<String> {

		private final List<ConfigurationProperty> propertiesWithMissingReplacement;

		private Report(List<ConfigurationProperty> propertiesWithMissingReplacement) {
			this.propertiesWithMissingReplacement = propertiesWithMissingReplacement;
		}

		private boolean hasProblems() {
			return !this.propertiesWithMissingReplacement.isEmpty();
		}

		@Override
		public Iterator<String> iterator() {
			List<String> lines = new ArrayList<>();
			if (this.propertiesWithMissingReplacement.isEmpty()) {
				lines.add("No problems found.");
			}
			else {
				lines.add("The following properties have a replacement that does not exist:");
				lines.add("");
				lines.addAll(this.propertiesWithMissingReplacement.stream()
					.map((property) -> "\t" + property.getName() + " (replacement "
							+ property.getDeprecation().replacement() + ")")
					.toList());
			}
			lines.add("");
			return lines.iterator();
		}

	}

}
