/*
 * Copyright 2012-2020 the original author or authors.
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
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Reader;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Properties;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.gradle.api.Task;
import org.gradle.api.file.FileCollection;
import org.gradle.api.internal.AbstractTask;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.TaskAction;

import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

/**
 * {@link Task} used to document test slices.
 *
 * @author Andy Wilkinson
 */
public class DocumentTestSlices extends AbstractTask {

	private FileCollection testSlices;

	private File outputFile;

	@InputFiles
	public FileCollection getTestSlices() {
		return this.testSlices;
	}

	public void setTestSlices(FileCollection testSlices) {
		this.testSlices = testSlices;
	}

	@OutputFile
	public File getOutputFile() {
		return this.outputFile;
	}

	public void setOutputFile(File outputFile) {
		this.outputFile = outputFile;
	}

	@TaskAction
	void documentTestSlices() throws IOException {
		Set<TestSlice> testSlices = readTestSlices();
		writeTable(testSlices);
	}

	@SuppressWarnings("unchecked")
	private Set<TestSlice> readTestSlices() throws IOException {
		Set<TestSlice> testSlices = new TreeSet<>();
		for (File metadataFile : this.testSlices) {
			Properties metadata = new Properties();
			try (Reader reader = new FileReader(metadataFile)) {
				metadata.load(reader);
			}
			for (String name : Collections.list((Enumeration<String>) metadata.propertyNames())) {
				testSlices.add(new TestSlice(name,
						new TreeSet<>(StringUtils.commaDelimitedListToSet(metadata.getProperty(name)))));
			}
		}
		return testSlices;
	}

	private void writeTable(Set<TestSlice> testSlices) throws IOException {
		this.outputFile.getParentFile().mkdirs();
		try (PrintWriter writer = new PrintWriter(new FileWriter(this.outputFile))) {
			writer.println("[cols=\"d,a\"]");
			writer.println("|===");
			writer.println("| Test slice | Imported auto-configuration");
			for (TestSlice testSlice : testSlices) {
				writer.println();
				writer.printf("| `@%s`%n", testSlice.className);
				writer.println("| ");
				for (String importedAutoConfiguration : testSlice.importedAutoConfigurations) {
					writer.printf("`%s`%n", importedAutoConfiguration);
				}
			}
			writer.println("|===");
		}
	}

	private static final class TestSlice implements Comparable<TestSlice> {

		private final String className;

		private final SortedSet<String> importedAutoConfigurations;

		private TestSlice(String className, SortedSet<String> importedAutoConfigurations) {
			this.className = ClassUtils.getShortName(className);
			this.importedAutoConfigurations = importedAutoConfigurations;
		}

		@Override
		public int compareTo(TestSlice other) {
			return this.className.compareTo(other.className);
		}

	}

}
