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
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.springframework.boot.build.context.properties.ConfigurationPropertiesAnalyzer.Analysis;
import org.springframework.boot.build.context.properties.ConfigurationPropertiesAnalyzer.Report;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Tests for {@link ConfigurationPropertiesAnalyzer}.
 *
 * @author Stephane Nicoll
 */
class ConfigurationPropertiesAnalyzerTests {

	@Test
	void createAnalyzerWithNoSource() {
		assertThatIllegalArgumentException()
			.isThrownBy(() -> new ConfigurationPropertiesAnalyzer(Collections.emptyList()))
			.withMessage("At least one source should be provided");
	}

	@Test
	void analyzeOrderWithAlphabeticalOrder(@TempDir File tempDir) throws IOException {
		File metadata = new File(tempDir, "metadata.json");
		Files.writeString(metadata.toPath(), """
				{ "properties": [
					{ "name": "abc"}, {"name": "def"}, {"name": "xyz"}
				  ]
				}""");
		Report report = new Report(tempDir);
		ConfigurationPropertiesAnalyzer analyzer = new ConfigurationPropertiesAnalyzer(List.of(metadata));
		analyzer.analyzeOrder(report);
		assertThat(report.hasProblems()).isFalse();
		assertThat(report.getAnalyses(metadata)).singleElement()
			.satisfies(((analysis) -> assertThat(analysis.getItems()).isEmpty()));
	}

	@Test
	void analyzeOrderWithViolations(@TempDir File tempDir) throws IOException {
		File metadata = new File(tempDir, "metadata.json");
		Files.writeString(metadata.toPath(), """
				{ "properties": [
					{ "name": "def"}, {"name": "abc"}, {"name": "xyz"}
				  ]
				}""");
		Report report = new Report(tempDir);
		ConfigurationPropertiesAnalyzer analyzer = new ConfigurationPropertiesAnalyzer(List.of(metadata));
		analyzer.analyzeOrder(report);
		assertThat(report.hasProblems()).isTrue();
		assertThat(report.getAnalyses(metadata)).singleElement()
			.satisfies((analysis) -> assertThat(analysis.getItems()).containsExactly(
					"Wrong order at $.properties[0].name - expected 'abc' but found 'def'",
					"Wrong order at $.properties[1].name - expected 'def' but found 'abc'"));
	}

	@Test
	void analyzeDuplicatesWithNoDuplicates(@TempDir File tempDir) throws IOException {
		File metadata = new File(tempDir, "metadata.json");
		Files.writeString(metadata.toPath(), """
				{ "properties": [
					{ "name": "abc"}, {"name": "def"}, {"name": "xyz"}
				  ]
				}""");
		Report report = new Report(tempDir);
		ConfigurationPropertiesAnalyzer analyzer = new ConfigurationPropertiesAnalyzer(List.of(metadata));
		analyzer.analyzeOrder(report);
		assertThat(report.hasProblems()).isFalse();
		assertThat(report.getAnalyses(metadata)).singleElement()
			.satisfies(((analysis) -> assertThat(analysis.getItems()).isEmpty()));
	}

	@Test
	void analyzeDuplicatesWithDuplicate(@TempDir File tempDir) throws IOException {
		File metadata = new File(tempDir, "metadata.json");
		Files.writeString(metadata.toPath(), """
				{ "properties": [
					{ "name": "abc"}, {"name": "abc"}, {"name": "def"}
				  ]
				}""");
		Report report = new Report(tempDir);
		ConfigurationPropertiesAnalyzer analyzer = new ConfigurationPropertiesAnalyzer(List.of(metadata));
		analyzer.analyzeDuplicates(report);
		assertThat(report.hasProblems()).isTrue();
		assertThat(report.getAnalyses(metadata)).singleElement()
			.satisfies((analysis) -> assertThat(analysis.getItems())
				.containsExactly("Duplicate name 'abc' at $.properties[1]"));
	}

	@Test
	void analyzePropertyDescription(@TempDir File tempDir) throws IOException {
		File metadata = new File(tempDir, "metadata.json");
		Files.writeString(metadata.toPath(), """
				{ "properties": [
					{ "name": "abc", "description": "This is abc." },
					{ "name": "def", "description": "This is def." },
					{ "name": "xyz", "description": "This is xyz." }
				  ]
				}""");
		Report report = new Report(tempDir);
		ConfigurationPropertiesAnalyzer analyzer = new ConfigurationPropertiesAnalyzer(List.of(metadata));
		analyzer.analyzePropertyDescription(report, List.of());
		assertThat(report.hasProblems()).isFalse();
		assertThat(report.getAnalyses(metadata)).singleElement()
			.satisfies(((analysis) -> assertThat(analysis.getItems()).isEmpty()));
	}

	@Test
	void analyzePropertyDescriptionWithMissingDescription(@TempDir File tempDir) throws IOException {
		File metadata = new File(tempDir, "metadata.json");
		Files.writeString(metadata.toPath(), """
				{ "properties": [
					{ "name": "abc", "description": "This is abc." },
					{ "name": "def" },
					{ "name": "xyz", "description": "This is xyz." }
				  ]
				}""");
		Report report = new Report(tempDir);
		ConfigurationPropertiesAnalyzer analyzer = new ConfigurationPropertiesAnalyzer(List.of(metadata));
		analyzer.analyzePropertyDescription(report, List.of());
		assertThat(report.hasProblems()).isTrue();
		assertThat(report.getAnalyses(metadata)).singleElement()
			.satisfies(((analysis) -> assertThat(analysis.getItems()).containsExactly("def")));
	}

	@Test
	void analyzeDeprecatedPropertyWithMissingSince(@TempDir File tempDir) throws IOException {
		File metadata = new File(tempDir, "metadata.json");
		Files.writeString(metadata.toPath(), """
				{ "properties": [
				  {
				    "name": "abc",
				    "description": "This is abc.",
				    "deprecation": { "reason": "abc reason", "since": "3.0.0" }
				  },
				  { "name": "def", "description": "This is def." },
				  {
				    "name": "xyz",
				    "description": "This is xyz.",
				    "deprecation": { "reason": "xyz reason" }
				  }
				  ]
				}""");
		Report report = new Report(tempDir);
		ConfigurationPropertiesAnalyzer analyzer = new ConfigurationPropertiesAnalyzer(List.of(metadata));
		analyzer.analyzeDeprecationSince(report);
		assertThat(report.hasProblems()).isTrue();
		assertThat(report.getAnalyses(metadata)).singleElement()
			.satisfies(((analysis) -> assertThat(analysis.getItems()).containsExactly("xyz")));
	}

	@Test
	void writeEmptyReport(@TempDir File tempDir) throws IOException {
		assertThat(writeToFile(tempDir, new Report(tempDir))).hasContent("No problems found.");
	}

	@Test
	void writeReportWithNoProblemsFound(@TempDir File tempDir) throws IOException {
		Report report = new Report(tempDir);
		File first = new File(tempDir, "metadata-1.json");
		report.registerAnalysis(first, new Analysis("Check for things:"));
		File second = new File(tempDir, "metadata-2.json");
		report.registerAnalysis(second, new Analysis("Check for other things:"));
		assertThat(writeToFile(tempDir, report)).content().isEqualToIgnoringNewLines("""
				metadata-1.json
				No problems found.

				metadata-2.json
				No problems found.
				""");
	}

	@Test
	void writeReportWithOneProblem(@TempDir File tempDir) throws IOException {
		Report report = new Report(tempDir);
		File metadata = new File(tempDir, "metadata-1.json");
		Analysis analysis = new Analysis("Check for things:");
		analysis.addItem("Should not be deprecated");
		report.registerAnalysis(metadata, analysis);
		report.registerAnalysis(metadata, new Analysis("Check for other things:"));
		assertThat(writeToFile(tempDir, report)).content().isEqualToIgnoringNewLines("""
				metadata-1.json
				Check for things:
					- Should not be deprecated

				Check for other things:
				No problems found.
				""");
	}

	@Test
	void writeReportWithSeveralProblems(@TempDir File tempDir) throws IOException {
		Report report = new Report(tempDir);
		File metadata = new File(tempDir, "metadata-1.json");
		Analysis firstAnalysis = new Analysis("Check for things:");
		firstAnalysis.addItem("Should not be deprecated");
		firstAnalysis.addItem("Should not be public");
		report.registerAnalysis(metadata, firstAnalysis);
		Analysis secondAnalysis = new Analysis("Check for other things:");
		secondAnalysis.addItem("Field 'this' not expected");
		report.registerAnalysis(metadata, secondAnalysis);
		assertThat(writeToFile(tempDir, report)).content().isEqualToIgnoringNewLines("""
				metadata-1.json
				Check for things:
					- Should not be deprecated
					- Should not be public

				Check for other things:
					- Field 'this' not expected
				""");
	}

	private File writeToFile(File directory, Report report) throws IOException {
		File file = new File(directory, "report.txt");
		report.write(file);
		return file;
	}

}
