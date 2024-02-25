/*
 * Copyright 2012-2023 the original author or authors.
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

import org.gradle.api.DefaultTask;
import org.gradle.api.Task;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.file.FileCollection;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.api.tasks.TaskAction;

import org.springframework.util.StringUtils;

/**
 * {@link Task} to document all starter projects.
 *
 * @author Andy Wilkinson
 */
public class DocumentStarters extends DefaultTask {

	private final Configuration starters;

	private File outputDir;

	/**
     * Initializes the DocumentStarters object.
     * 
     * This method creates the "starters" configuration and adds dependencies to it based on the projects evaluated by Gradle.
     * If a project has the StarterPlugin applied, its path and "starterMetadata" configuration are added as dependencies to the "starters" configuration.
     */
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

	/**
     * Returns the output directory.
     *
     * @return the output directory
     */
    @OutputDirectory
	public File getOutputDir() {
		return this.outputDir;
	}

	/**
     * Sets the output directory for the DocumentStarters class.
     * 
     * @param outputDir the output directory to be set
     */
    public void setOutputDir(File outputDir) {
		this.outputDir = outputDir;
	}

	/**
     * Returns the collection of starter files.
     * 
     * @return The collection of starter files.
     */
    @InputFiles
	@PathSensitive(PathSensitivity.RELATIVE)
	public FileCollection getStarters() {
		return this.starters;
	}

	/**
     * This method documents the starters in the application.
     * It retrieves the list of starter files and loads them into a set.
     * The starters are then categorized into application, production, and technical starters.
     * Finally, the categorized starters are written into separate tables.
     */
    @TaskAction
	void documentStarters() {
		Set<Starter> starters = this.starters.getFiles()
			.stream()
			.map(this::loadStarter)
			.collect(Collectors.toCollection(TreeSet::new));
		writeTable("application-starters", starters.stream().filter(Starter::isApplication));
		writeTable("production-starters", starters.stream().filter(Starter::isProduction));
		writeTable("technical-starters", starters.stream().filter(Starter::isTechnical));
	}

	/**
     * Loads a Starter object from a metadata file.
     * 
     * @param metadata the metadata file to load the Starter from
     * @return the loaded Starter object
     * @throws RuntimeException if an IOException occurs while reading the metadata file
     */
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

	/**
     * Writes a table of starters to a file in AsciiDoc format.
     * 
     * @param name     the name of the table
     * @param starters a stream of Starter objects
     * @throws RuntimeException if an I/O error occurs
     */
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

	/**
     * Post-processes the description by adding starter cross-links.
     * 
     * @param description the original description
     * @return the description with starter cross-links added
     */
    private String postProcessDescription(String description) {
		return addStarterCrossLinks(description);
	}

	/**
     * Adds starter cross-links to the given input string.
     * 
     * @param input the input string to add starter cross-links to
     * @return the modified input string with starter cross-links added
     */
    private String addStarterCrossLinks(String input) {
		return input.replaceAll("(spring-boot-starter[A-Za-z-]*)", "<<$1,`$1`>>");
	}

	/**
     * Starter class.
     */
    private static final class Starter implements Comparable<Starter> {

		private final String name;

		private final String description;

		private final Set<String> dependencies;

		/**
         * Constructs a new Starter object with the specified name, description, and dependencies.
         * 
         * @param name the name of the starter
         * @param description the description of the starter
         * @param dependencies the set of dependencies required by the starter
         */
        private Starter(String name, String description, Set<String> dependencies) {
			this.name = name;
			this.description = description;
			this.dependencies = dependencies;
		}

		/**
         * Checks if the current instance is in production mode.
         * 
         * @return true if the current instance is in production mode, false otherwise.
         */
        private boolean isProduction() {
			return this.name.equals("spring-boot-starter-actuator");
		}

		/**
         * Checks if the starter is technical.
         * 
         * @return true if the starter is technical, false otherwise
         */
        private boolean isTechnical() {
			return !Arrays.asList("spring-boot-starter", "spring-boot-starter-test").contains(this.name)
					&& !isProduction() && !this.dependencies.contains("spring-boot-starter");
		}

		/**
         * Checks if the application is running in a non-production and non-technical environment.
         * 
         * @return true if the application is running in a non-production and non-technical environment, false otherwise.
         */
        private boolean isApplication() {
			return !isProduction() && !isTechnical();
		}

		/**
         * Compares this Starter object with the specified Starter object for order based on their names.
         * 
         * @param other the other Starter object to be compared
         * @return a negative integer, zero, or a positive integer as this Starter object is less than, equal to, or greater than the specified Starter object
         */
        @Override
		public int compareTo(Starter other) {
			return this.name.compareTo(other.name);
		}

	}

}
