/*
 * Copyright 2012-2022 the original author or authors.
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
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.gradle.api.GradleException;
import org.gradle.api.file.FileTree;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.api.tasks.SourceTask;
import org.gradle.api.tasks.TaskAction;

/**
 * {@link SourceTask} that checks additional Spring configuration metadata files.
 *
 * @author Andy Wilkinson
 */
public class CheckAdditionalSpringConfigurationMetadata extends SourceTask {

	private final RegularFileProperty reportLocation;

	public CheckAdditionalSpringConfigurationMetadata() {
		this.reportLocation = getProject().getObjects().fileProperty();
	}

	@OutputFile
	public RegularFileProperty getReportLocation() {
		return this.reportLocation;
	}

	@Override
	@InputFiles
	@PathSensitive(PathSensitivity.RELATIVE)
	public FileTree getSource() {
		return super.getSource();
	}

	@TaskAction
	void check() throws JsonParseException, IOException {
		Report report = createReport();
		File reportFile = getReportLocation().get().getAsFile();
		Files.write(reportFile.toPath(), report, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
		if (report.hasProblems()) {
			throw new GradleException(
					"Problems found in additional Spring configuration metadata. See " + reportFile + " for details.");
		}
	}

	@SuppressWarnings("unchecked")
	private Report createReport() throws IOException, JsonParseException, JsonMappingException {
		ObjectMapper objectMapper = new ObjectMapper();
		Report report = new Report();
		for (File file : getSource().getFiles()) {
			Analysis analysis = report.analysis(getProject().getProjectDir().toPath().relativize(file.toPath()));
			Map<String, Object> json = objectMapper.readValue(file, Map.class);
			check("groups", json, analysis);
			check("properties", json, analysis);
			check("hints", json, analysis);
		}
		return report;
	}

	@SuppressWarnings("unchecked")
	private void check(String key, Map<String, Object> json, Analysis analysis) {
		List<Map<String, Object>> groups = (List<Map<String, Object>>) json.get(key);
		List<String> names = groups.stream().map((group) -> (String) group.get("name")).toList();
		List<String> sortedNames = sortedCopy(names);
		for (int i = 0; i < names.size(); i++) {
			String actual = names.get(i);
			String expected = sortedNames.get(i);
			if (!actual.equals(expected)) {
				analysis.problems.add("Wrong order at $." + key + "[" + i + "].name - expected '" + expected
						+ "' but found '" + actual + "'");
			}
		}
	}

	private List<String> sortedCopy(Collection<String> original) {
		List<String> copy = new ArrayList<>(original);
		Collections.sort(copy);
		return copy;
	}

	private static final class Report implements Iterable<String> {

		private final List<Analysis> analyses = new ArrayList<>();

		private Analysis analysis(Path path) {
			Analysis analysis = new Analysis(path);
			this.analyses.add(analysis);
			return analysis;
		}

		private boolean hasProblems() {
			for (Analysis analysis : this.analyses) {
				if (!analysis.problems.isEmpty()) {
					return true;
				}
			}
			return false;
		}

		@Override
		public Iterator<String> iterator() {
			List<String> lines = new ArrayList<>();
			for (Analysis analysis : this.analyses) {
				lines.add(analysis.source.toString());
				lines.add("");
				if (analysis.problems.isEmpty()) {
					lines.add("No problems found.");
				}
				else {
					lines.addAll(analysis.problems);
				}
				lines.add("");
			}
			return lines.iterator();
		}

	}

	private static final class Analysis {

		private final List<String> problems = new ArrayList<>();

		private final Path source;

		private Analysis(Path source) {
			this.source = source;
		}

	}

}
