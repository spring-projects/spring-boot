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
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import tools.jackson.core.StreamReadFeature;
import tools.jackson.databind.json.JsonMapper;

import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.function.SingletonSupplier;

/**
 * Check configuration metadata for inconsistencies. The available checks are:
 * <ul>
 * <li>Metadata element should be sorted alphabetically: {@link #analyzeSort(Report)}</li>
 * <li>Property must have a description:
 * {@link #analyzePropertyDescription(Report, List)}</li>
 * </ul>
 *
 * @author Stephane Nicoll
 */
class ConfigurationPropertiesAnalyzer {

	private final Collection<File> sources;

	private final SingletonSupplier<JsonMapper> jsonMapperSupplier;

	ConfigurationPropertiesAnalyzer(Collection<File> sources) {
		if (sources.isEmpty()) {
			throw new IllegalArgumentException("At least one source should be provided");
		}
		this.sources = sources;
		this.jsonMapperSupplier = SingletonSupplier
			.of(() -> JsonMapper.builder().enable(StreamReadFeature.INCLUDE_SOURCE_IN_LOCATION).build());
	}

	void analyzeSort(Report report) {
		for (File source : this.sources) {
			report.registerAnalysis(source, analyzeSort(source));
		}
	}

	private Analysis analyzeSort(File source) {
		Map<String, Object> json = readJsonContent(source);
		Analysis analysis = new Analysis("Metadata element order:");
		analyzeMetadataElementsSort("groups", json, analysis);
		analyzeMetadataElementsSort("properties", json, analysis);
		analyzeMetadataElementsSort("hints", json, analysis);
		return analysis;
	}

	@SuppressWarnings("unchecked")
	private void analyzeMetadataElementsSort(String key, Map<String, Object> json, Analysis analysis) {
		List<Map<String, Object>> groups = (List<Map<String, Object>>) json.getOrDefault(key, Collections.emptyList());
		List<String> names = groups.stream().map((group) -> (String) group.get("name")).toList();
		List<String> sortedNames = names.stream().sorted().toList();
		for (int i = 0; i < names.size(); i++) {
			String actual = names.get(i);
			String expected = sortedNames.get(i);
			if (!actual.equals(expected)) {
				analysis.addItem("Wrong order at $." + key + "[" + i + "].name - expected '" + expected
						+ "' but found '" + actual + "'");
			}
		}
	}

	void analyzePropertyDescription(Report report, List<String> exclusions) {
		for (File source : this.sources) {
			report.registerAnalysis(source, analyzePropertyDescription(source, exclusions));
		}
	}

	@SuppressWarnings("unchecked")
	private Analysis analyzePropertyDescription(File source, List<String> exclusions) {
		Map<String, Object> json = readJsonContent(source);
		Analysis analysis = new Analysis("The following properties have no description:");
		List<Map<String, Object>> properties = (List<Map<String, Object>>) json.get("properties");
		for (Map<String, Object> property : properties) {
			String name = (String) property.get("name");
			if (!isDeprecated(property) && !isDescribed(property) && !isExcluded(exclusions, name)) {
				analysis.addItem(name);
			}
		}
		return analysis;
	}

	private boolean isExcluded(List<String> exclusions, String propertyName) {
		for (String exclusion : exclusions) {
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

	private boolean isDeprecated(Map<String, Object> property) {
		return property.get("deprecation") != null;
	}

	private boolean isDescribed(Map<String, Object> property) {
		return property.get("description") != null;
	}

	void analyzeDeprecationSince(Report report) throws IOException {
		for (File source : this.sources) {
			report.registerAnalysis(source, analyzeDeprecationSince(source));
		}
	}

	@SuppressWarnings("unchecked")
	private Analysis analyzeDeprecationSince(File source) throws IOException {
		Analysis analysis = new Analysis("The following properties are deprecated without a 'since' version:");
		Map<String, Object> json = readJsonContent(source);
		List<Map<String, Object>> properties = (List<Map<String, Object>>) json.get("properties");
		properties.stream().filter((property) -> property.containsKey("deprecation")).forEach((property) -> {
			Map<String, Object> deprecation = (Map<String, Object>) property.get("deprecation");
			if (!deprecation.containsKey("since")) {
				analysis.addItem(property.get("name").toString());
			}
		});
		return analysis;
	}

	@SuppressWarnings("unchecked")
	private Map<String, Object> readJsonContent(File source) {
		return this.jsonMapperSupplier.obtain().readValue(source, Map.class);
	}

	private static <T> void writeAll(PrintWriter writer, Iterable<T> elements, Consumer<T> itemWriter) {
		Iterator<T> it = elements.iterator();
		while (it.hasNext()) {
			itemWriter.accept(it.next());
			if (it.hasNext()) {
				writer.println();
			}
		}
	}

	static class Report {

		private final File baseDirectory;

		private final MultiValueMap<File, Analysis> analyses = new LinkedMultiValueMap<>();

		Report(File baseDirectory) {
			this.baseDirectory = baseDirectory;
		}

		void registerAnalysis(File path, Analysis analysis) {
			this.analyses.add(path, analysis);
		}

		boolean hasProblems() {
			return this.analyses.values()
				.stream()
				.anyMatch((candidates) -> candidates.stream().anyMatch(Analysis::hasProblems));
		}

		List<Analysis> getAnalyses(File source) {
			return this.analyses.getOrDefault(source, Collections.emptyList());
		}

		/**
		 * Write this report to the given {@code file}.
		 * @param file the file to write the report to
		 */
		void write(File file) throws IOException {
			Files.writeString(file.toPath(), createContent(), StandardOpenOption.CREATE,
					StandardOpenOption.TRUNCATE_EXISTING);
		}

		private String createContent() {
			if (this.analyses.isEmpty()) {
				return "No problems found.";
			}
			StringWriter out = new StringWriter();
			try (PrintWriter writer = new PrintWriter(out)) {
				writeAll(writer, this.analyses.entrySet(), (entry) -> {
					writer.println(this.baseDirectory.toPath().relativize(entry.getKey().toPath()));
					boolean hasProblems = entry.getValue().stream().anyMatch(Analysis::hasProblems);
					if (hasProblems) {
						writeAll(writer, entry.getValue(), (analysis) -> analysis.createDetails(writer));
					}
					else {
						writer.println("No problems found.");
					}
				});
			}
			return out.toString();
		}

	}

	static class Analysis {

		private final String header;

		private final List<String> items;

		Analysis(String header) {
			this.header = header;
			this.items = new ArrayList<>();
		}

		void addItem(String item) {
			this.items.add(item);
		}

		boolean hasProblems() {
			return !this.items.isEmpty();
		}

		List<String> getItems() {
			return this.items;
		}

		void createDetails(PrintWriter writer) {
			writer.println(this.header);
			if (this.items.isEmpty()) {
				writer.println("No problems found.");
			}
			else {
				for (String item : this.items) {
					writer.println("\t- " + item);
				}
			}
		}

		@Override
		public String toString() {
			StringWriter out = new StringWriter();
			PrintWriter writer = new PrintWriter(out);
			createDetails(writer);
			return out.toString();
		}

	}

}
