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

package org.springframework.boot.build.autoconfigure;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Reader;
import java.util.Properties;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.gradle.api.Task;
import org.gradle.api.file.FileCollection;
import org.gradle.api.internal.AbstractTask;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.TaskAction;

import org.springframework.util.StringUtils;

/**
 * {@link Task} used to document auto-configuration classes.
 *
 * @author Andy Wilkinson
 */
public class DocumentAutoConfigurationClasses extends AbstractTask {

	private FileCollection autoConfiguration;

	private File outputDir;

	@InputFiles
	public FileCollection getAutoConfiguration() {
		return this.autoConfiguration;
	}

	public void setAutoConfiguration(FileCollection autoConfiguration) {
		this.autoConfiguration = autoConfiguration;
	}

	@OutputDirectory
	public File getOutputDir() {
		return this.outputDir;
	}

	public void setOutputDir(File outputDir) {
		this.outputDir = outputDir;
	}

	@TaskAction
	void documentAutoConfigurationClasses() throws IOException {
		for (File metadataFile : this.autoConfiguration) {
			Properties metadata = new Properties();
			try (Reader reader = new FileReader(metadataFile)) {
				metadata.load(reader);
			}
			AutoConfiguration autoConfiguration = new AutoConfiguration(metadata.getProperty("module"), new TreeSet<>(
					StringUtils.commaDelimitedListToSet(metadata.getProperty("autoConfigurationClassNames"))));
			writeTable(autoConfiguration);
		}
	}

	private void writeTable(AutoConfiguration autoConfigurationClasses) throws IOException {
		this.outputDir.mkdirs();
		try (PrintWriter writer = new PrintWriter(
				new FileWriter(new File(this.outputDir, autoConfigurationClasses.module + ".adoc")))) {
			writer.println("[cols=\"4,1\"]");
			writer.println("|===");
			writer.println("| Configuration Class | Links");

			for (AutoConfigurationClass autoConfigurationClass : autoConfigurationClasses.classes) {
				writer.println();
				writer.printf("| {spring-boot-code}/spring-boot-project/%s/src/main/java/%s.java[`%s`]%n",
						autoConfigurationClasses.module, autoConfigurationClass.path, autoConfigurationClass.name);
				writer.printf("| {spring-boot-api}/%s.html[javadoc]%n", autoConfigurationClass.path);
			}

			writer.println("|===");
		}
	}

	private static final class AutoConfiguration {

		private final String module;

		private final SortedSet<AutoConfigurationClass> classes;

		private AutoConfiguration(String module, Set<String> classNames) {
			this.module = module;
			this.classes = classNames.stream().map((className) -> {
				String path = className.replace('.', '/');
				String name = className.substring(className.lastIndexOf('.') + 1);
				return new AutoConfigurationClass(name, path);
			}).collect(Collectors.toCollection(TreeSet::new));
		}

	}

	private static final class AutoConfigurationClass implements Comparable<AutoConfigurationClass> {

		private final String name;

		private final String path;

		private AutoConfigurationClass(String name, String path) {
			this.name = name;
			this.path = path;
		}

		@Override
		public int compareTo(AutoConfigurationClass other) {
			return this.name.compareTo(other.name);
		}

	}

}
