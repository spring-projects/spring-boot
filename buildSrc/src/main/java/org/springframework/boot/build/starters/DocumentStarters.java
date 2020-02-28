/*
 * Copyright 2019-2020 the original author or authors.
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

package org.springframework.boot.build.starters;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.gradle.api.Task;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.file.FileCollection;
import org.gradle.api.internal.AbstractTask;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.TaskAction;

import org.springframework.util.StringUtils;

/**
 * {@link Task} to document all starter projects.
 *
 * @author Andy Wilkinson
 */
public class DocumentStarters extends AbstractTask {

	private final Configuration starters;

	private File outputDir;

	public DocumentStarters() {
		this.starters = getProject().getConfigurations().create("starters");
		getProject().getGradle().projectsEvaluated((gradle) -> {
			gradle.allprojects((project) -> {
				if (project.getPlugins().hasPlugin(StarterPlugin.class)) {
					Map<String, String> dependency = new HashMap<>();
					dependency.put("path", project.getPath());
					dependency.put("configuration", "starterMetadata");
					this.starters.getDependencies().add(project.getDependencies().project(dependency));
				}
			});
		});
	}

	@OutputDirectory
	public File getOutputDir() {
		return this.outputDir;
	}

	public void setOutputDir(File outputDir) {
		this.outputDir = outputDir;
	}

	@InputFiles
	public FileCollection getStarters() {
		return this.starters;
	}

	@TaskAction
	void documentStarters() {
		Set<Starter> starters = this.starters.getFiles().stream().map(this::loadStarter)
				.collect(Collectors.toCollection(TreeSet::new));
		writeTable("application-starters", starters.stream().filter(Starter::isApplication));
		writeTable("production-starters", starters.stream().filter(Starter::isProduction));
		writeTable("technical-starters", starters.stream().filter(Starter::isTechnical));
	}

	private Starter loadStarter(File metadata) {
		Properties properties = new Properties();
		try (FileReader reader = new FileReader(metadata)) {
			properties.load(reader);
			return new Starter(properties.getProperty("name"), properties.getProperty("description"),
					StringUtils.commaDelimitedListToSet(properties.getProperty("dependencies")));
		}
		catch (IOException ex) {
			throw new RuntimeException(ex);
		}
	}

	private void writeTable(String name, Stream<Starter> starters) {
		File output = new File(this.outputDir, name + ".adoc");
		output.getParentFile().mkdirs();
		try (PrintWriter writer = new PrintWriter(new FileWriter(output))) {
			writer.println("|===");
			writer.println("| Name | Description");
			starters.forEach((starter) -> {
				writer.println();
				writer.printf("| [[%s]]`%s`%n", starter.name, starter.name);
				writer.printf("| %s%n", postProcessDescription(starter.description));
			});
			writer.println("|===");
		}
		catch (IOException ex) {
			throw new RuntimeException(ex);
		}
	}

	private String postProcessDescription(String description) {
		return addStarterCrossLinks(description);
	}

	private String addStarterCrossLinks(String input) {
		return input.replaceAll("(spring-boot-starter[A-Za-z-]*)", "<<$1,`$1`>>");
	}

	private static final class Starter implements Comparable<Starter> {

		private final String name;

		private final String description;

		private final Set<String> dependencies;

		private Starter(String name, String description, Set<String> dependencies) {
			this.name = name;
			this.description = description;
			this.dependencies = dependencies;
		}

		private boolean isProduction() {
			return this.name.equals("spring-boot-starter-actuator");
		}

		private boolean isTechnical() {
			return !Arrays.asList("spring-boot-starter", "spring-boot-starter-test").contains(this.name)
					&& !isProduction() && !this.dependencies.contains("spring-boot-starter");
		}

		private boolean isApplication() {
			return !isProduction() && !isTechnical();
		}

		@Override
		public int compareTo(Starter other) {
			return this.name.compareTo(other.name);
		}

	}

}
