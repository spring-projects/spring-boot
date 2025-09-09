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

package org.springframework.boot.build.test.autoconfigure;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.gradle.api.DefaultTask;
import org.gradle.api.Task;
import org.gradle.api.file.FileCollection;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.api.tasks.TaskAction;
import tools.jackson.databind.json.JsonMapper;

import org.springframework.boot.build.test.autoconfigure.TestSliceMetadata.TestSlice;

/**
 * {@link Task} used to document test slices.
 *
 * @author Andy Wilkinson
 */
public abstract class DocumentTestSlices extends DefaultTask {

	private FileCollection testSliceMetadata;

	@InputFiles
	@PathSensitive(PathSensitivity.RELATIVE)
	public FileCollection getTestSlices() {
		return this.testSliceMetadata;
	}

	public void setTestSlices(FileCollection testSlices) {
		this.testSliceMetadata = testSlices;
	}

	@OutputFile
	public abstract RegularFileProperty getOutputFile();

	@TaskAction
	void documentTestSlices() throws IOException {
		Map<String, List<TestSlice>> testSlices = readTestSlices();
		writeTable(testSlices);
	}

	private Map<String, List<TestSlice>> readTestSlices() {
		Map<String, List<TestSlice>> testSlices = new TreeMap<>();
		for (File metadataFile : this.testSliceMetadata) {
			JsonMapper mapper = JsonMapper.builder().build();
			TestSliceMetadata metadata = mapper.readValue(metadataFile, TestSliceMetadata.class);
			List<TestSlice> slices = new ArrayList<>(metadata.testSlices());
			Collections.sort(slices, (s1, s2) -> s1.annotation().compareTo(s2.annotation()));
			testSlices.put(metadata.module(), slices);
		}
		return testSlices;
	}

	private void writeTable(Map<String, List<TestSlice>> testSlicesByModule) throws IOException {
		File outputFile = getOutputFile().getAsFile().get();
		outputFile.getParentFile().mkdirs();
		try (PrintWriter writer = new PrintWriter(new FileWriter(outputFile))) {
			writer.println("[cols=\"d,d,a\"]");
			writer.println("|===");
			writer.println("|Module | Test slice | Imported auto-configuration");
			testSlicesByModule.forEach((module, testSlices) -> {
				testSlices.forEach((testSlice) -> {
					writer.println();
					writer.printf("| `%s`%n", module);
					writer.printf("| javadoc:%s[format=annotation]%n", testSlice.annotation());
					writer.println("| ");
					for (String importedAutoConfiguration : testSlice.importedAutoConfigurations()) {
						writer.printf("`%s`%n", importedAutoConfiguration);
					}
				});
			});
			writer.println("|===");
		}
	}

}
