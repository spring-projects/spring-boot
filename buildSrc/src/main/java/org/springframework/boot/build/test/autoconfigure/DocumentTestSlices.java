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

import org.gradle.api.DefaultTask;
import org.gradle.api.Task;
import org.gradle.api.file.FileCollection;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.api.tasks.TaskAction;

import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

/**
 * {@link Task} used to document test slices.
 *
 * @author Andy Wilkinson
 */
public class DocumentTestSlices extends DefaultTask {

	private FileCollection testSlices;

	private File outputFile;

	/**
	 * Returns the collection of test slices.
	 * @return The collection of test slices.
	 */
	@InputFiles
	@PathSensitive(PathSensitivity.RELATIVE)
	public FileCollection getTestSlices() {
		return this.testSlices;
	}

	/**
	 * Sets the test slices for the DocumentTestSlices class.
	 * @param testSlices the test slices to be set
	 */
	public void setTestSlices(FileCollection testSlices) {
		this.testSlices = testSlices;
	}

	/**
	 * Returns the output file.
	 * @return the output file
	 */
	@OutputFile
	public File getOutputFile() {
		return this.outputFile;
	}

	/**
	 * Sets the output file for the document.
	 * @param outputFile the file to set as the output file
	 */
	public void setOutputFile(File outputFile) {
		this.outputFile = outputFile;
	}

	/**
	 * This method is used to document the test slices. It reads the test slices from a
	 * source and writes them to a table.
	 * @throws IOException if there is an error reading or writing the test slices.
	 */
	@TaskAction
	void documentTestSlices() throws IOException {
		Set<TestSlice> testSlices = readTestSlices();
		writeTable(testSlices);
	}

	/**
	 * Reads the test slices from the specified test slice files.
	 * @return a set of TestSlice objects representing the test slices
	 * @throws IOException if an I/O error occurs while reading the test slice files
	 */
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

	/**
	 * Writes the test slices and their imported auto-configurations to a table in the
	 * output file.
	 * @param testSlices the set of test slices to write
	 * @throws IOException if an I/O error occurs while writing to the output file
	 */
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

	/**
	 * TestSlice class.
	 */
	private static final class TestSlice implements Comparable<TestSlice> {

		private final String className;

		private final SortedSet<String> importedAutoConfigurations;

		/**
		 * Constructs a new TestSlice object with the specified class name and imported
		 * auto configurations.
		 * @param className the name of the class
		 * @param importedAutoConfigurations the set of imported auto configurations
		 */
		private TestSlice(String className, SortedSet<String> importedAutoConfigurations) {
			this.className = ClassUtils.getShortName(className);
			this.importedAutoConfigurations = importedAutoConfigurations;
		}

		/**
		 * Compares this TestSlice object with the specified TestSlice object for order.
		 * Returns a negative integer, zero, or a positive integer as this object is less
		 * than, equal to, or greater than the specified object.
		 * @param other the TestSlice object to be compared
		 * @return a negative integer, zero, or a positive integer as this object is less
		 * than, equal to, or greater than the specified object
		 */
		@Override
		public int compareTo(TestSlice other) {
			return this.className.compareTo(other.className);
		}

	}

}
