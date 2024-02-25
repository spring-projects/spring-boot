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

package org.springframework.boot.cli.command.init;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import joptsimple.OptionSet;
import joptsimple.OptionSpec;

import org.springframework.boot.cli.command.Command;
import org.springframework.boot.cli.command.HelpExample;
import org.springframework.boot.cli.command.OptionParsingCommand;
import org.springframework.boot.cli.command.options.OptionHandler;
import org.springframework.boot.cli.command.status.ExitStatus;
import org.springframework.boot.cli.util.Log;
import org.springframework.util.Assert;

/**
 * {@link Command} that initializes a project using Spring initializr.
 *
 * @author Stephane Nicoll
 * @author Eddú Meléndez
 * @author Vignesh Thangavel Ilangovan
 * @since 1.2.0
 */
public class InitCommand extends OptionParsingCommand {

	/**
     * Constructs a new InitCommand object.
     * 
     * This constructor initializes the InitCommand object by creating a new InitOptionHandler object
     * with a new InitializrService object as its parameter. The InitOptionHandler is then passed as a
     * parameter to the superclass constructor.
     */
    public InitCommand() {
		this(new InitOptionHandler(new InitializrService()));
	}

	/**
     * Constructs a new InitCommand object with the specified InitOptionHandler.
     * 
     * @param handler the InitOptionHandler to handle the initialization options
     */
    public InitCommand(InitOptionHandler handler) {
		super("init", "Initialize a new project using Spring Initializr (start.spring.io)", handler);
	}

	/**
     * Returns the usage help for the InitCommand.
     * 
     * @return the usage help string in the format "[options] [location]"
     */
    @Override
	public String getUsageHelp() {
		return "[options] [location]";
	}

	/**
     * Returns a collection of help examples for the getExamples() method.
     *
     * @return a collection of HelpExample objects representing different usage examples
     */
    @Override
	public Collection<HelpExample> getExamples() {
		List<HelpExample> examples = new ArrayList<>();
		examples.add(new HelpExample("To list all the capabilities of the service", "spring init --list"));
		examples.add(new HelpExample("To creates a default project", "spring init"));
		examples.add(new HelpExample("To create a web my-app.zip", "spring init -d=web my-app.zip"));
		examples.add(new HelpExample("To create a web/data-jpa gradle project unpacked",
				"spring init -d=web,jpa --build=gradle my-dir"));
		return examples;
	}

	/**
	 * {@link OptionHandler} for {@link InitCommand}.
	 */
	static class InitOptionHandler extends OptionHandler {

		/**
		 * Mapping from camelCase options advertised by the service to our kebab-case
		 * options.
		 */
		private static final Map<String, String> CAMEL_CASE_OPTIONS;
		static {
			Map<String, String> options = new HashMap<>();
			options.put("--groupId", "--group-id");
			options.put("--artifactId", "--artifact-id");
			options.put("--packageName", "--package-name");
			options.put("--javaVersion", "--java-version");
			options.put("--bootVersion", "--boot-version");
			CAMEL_CASE_OPTIONS = Collections.unmodifiableMap(options);
		}

		private final ServiceCapabilitiesReportGenerator serviceCapabilitiesReport;

		private final ProjectGenerator projectGenerator;

		private OptionSpec<String> target;

		private OptionSpec<Void> listCapabilities;

		private OptionSpec<String> groupId;

		private OptionSpec<String> artifactId;

		private OptionSpec<String> version;

		private OptionSpec<String> name;

		private OptionSpec<String> description;

		private OptionSpec<String> packageName;

		private OptionSpec<String> type;

		private OptionSpec<String> packaging;

		private OptionSpec<String> build;

		private OptionSpec<String> format;

		private OptionSpec<String> javaVersion;

		private OptionSpec<String> language;

		private OptionSpec<String> bootVersion;

		private OptionSpec<String> dependencies;

		private OptionSpec<Void> extract;

		private OptionSpec<Void> force;

		/**
         * Initializes the option handler with the given initializr service.
         * 
         * @param initializrService the initializr service to be used
         */
        InitOptionHandler(InitializrService initializrService) {
			super(InitOptionHandler::processArgument);
			this.serviceCapabilitiesReport = new ServiceCapabilitiesReportGenerator(initializrService);
			this.projectGenerator = new ProjectGenerator(initializrService);
		}

		/**
         * This method is used to set the options for the InitOptionHandler class.
         * It sets the target URL of the service to use, and also provides the option to list the capabilities of the service.
         * The target URL is a required argument and is set to the default service URL if not provided.
         * The listCapabilities option is used to discover the dependencies and types available.
         * This method also calls the projectGenerationOptions() and otherOptions() methods to set additional options.
         */
        @Override
		protected void options() {
			this.target = option(Arrays.asList("target"), "URL of the service to use").withRequiredArg()
				.defaultsTo(ProjectGenerationRequest.DEFAULT_SERVICE_URL);
			this.listCapabilities = option(Arrays.asList("list"),
					"List the capabilities of the service. Use it to discover the "
							+ "dependencies and the types that are available");
			projectGenerationOptions();
			otherOptions();
		}

		/**
         * Sets up the project generation options.
         * 
         * This method initializes the various options for generating a project. These options include the group ID, artifact ID,
         * version, name, description, package name, type, packaging, build system, format, Java version, language, Spring Boot version,
         * and dependencies.
         * 
         * The group ID option is used to specify the project coordinates, which is typically in the format 'org.test'.
         * 
         * The artifact ID option is used to specify the project coordinates and infer the archive name, which is typically in the format 'test'.
         * 
         * The version option is used to specify the project version, which is typically in the format '0.0.1-SNAPSHOT'.
         * 
         * The name option is used to specify the project name and infer the application name.
         * 
         * The description option is used to specify the project description.
         * 
         * The package name option is used to specify the package name.
         * 
         * The type option is used to specify the project type. This option is not normally needed if the --build and/or --format options are used.
         * 
         * The packaging option is used to specify the project packaging, which is typically in the format 'jar'.
         * 
         * The build option is used to specify the build system to use, which is typically 'maven' or 'gradle'. The default value is 'maven'.
         * 
         * The format option is used to specify the format of the generated content. This option can be 'build' for a build file or 'project' for a project archive. The default value is 'project'.
         * 
         * The Java version option is used to specify the language level, which is typically '1.8'.
         * 
         * The language option is used to specify the programming language, which is typically 'java'.
         * 
         * The Spring Boot version option is used to specify the Spring Boot version, which is typically '1.2.0.RELEASE'.
         * 
         * The dependencies option is used to specify a comma-separated list of dependency identifiers to include in the generated project.
         */
        private void projectGenerationOptions() {
			this.groupId = option(Arrays.asList("group-id", "g"), "Project coordinates (for example 'org.test')")
				.withRequiredArg();
			this.artifactId = option(Arrays.asList("artifact-id", "a"),
					"Project coordinates; infer archive name (for example 'test')")
				.withRequiredArg();
			this.version = option(Arrays.asList("version", "v"), "Project version (for example '0.0.1-SNAPSHOT')")
				.withRequiredArg();
			this.name = option(Arrays.asList("name", "n"), "Project name; infer application name").withRequiredArg();
			this.description = option("description", "Project description").withRequiredArg();
			this.packageName = option(Arrays.asList("package-name"), "Package name").withRequiredArg();
			this.type = option(Arrays.asList("type", "t"),
					"Project type. Not normally needed if you use --build "
							+ "and/or --format. Check the capabilities of the service (--list) for more details")
				.withRequiredArg();
			this.packaging = option(Arrays.asList("packaging", "p"), "Project packaging (for example 'jar')")
				.withRequiredArg();
			this.build = option("build", "Build system to use (for example 'maven' or 'gradle')").withRequiredArg()
				.defaultsTo("maven");
			this.format = option("format", "Format of the generated content (for example 'build' for a build file, "
					+ "'project' for a project archive)")
				.withRequiredArg()
				.defaultsTo("project");
			this.javaVersion = option(Arrays.asList("java-version", "j"), "Language level (for example '1.8')")
				.withRequiredArg();
			this.language = option(Arrays.asList("language", "l"), "Programming language  (for example 'java')")
				.withRequiredArg();
			this.bootVersion = option(Arrays.asList("boot-version", "b"),
					"Spring Boot version (for example '1.2.0.RELEASE')")
				.withRequiredArg();
			this.dependencies = option(Arrays.asList("dependencies", "d"),
					"Comma-separated list of dependency identifiers to include in the generated project")
				.withRequiredArg();
		}

		/**
         * Sets up other options for the InitOptionHandler class.
         * This method initializes the 'extract' and 'force' options.
         * The 'extract' option is used to extract the project archive.
         * It is inferred if a location is specified without an extension.
         * The 'force' option is used to force overwrite of existing files.
         */
        private void otherOptions() {
			this.extract = option(Arrays.asList("extract", "x"),
					"Extract the project archive. Inferred if a location is specified without an extension");
			this.force = option(Arrays.asList("force", "f"), "Force overwrite of existing files");
		}

		/**
         * Executes the run method with the given options.
         * 
         * @param options the options provided by the user
         * @return the exit status of the execution
         * @throws Exception if an error occurs during execution
         */
        @Override
		protected ExitStatus run(OptionSet options) throws Exception {
			try {
				if (options.has(this.listCapabilities)) {
					generateReport(options);
				}
				else {
					generateProject(options);
				}
				return ExitStatus.OK;
			}
			catch (ReportableException ex) {
				Log.error(ex.getMessage());
				return ExitStatus.ERROR;
			}
			catch (Exception ex) {
				Log.error(ex);
				return ExitStatus.ERROR;
			}
		}

		/**
         * Generates a report based on the given options.
         * 
         * @param options the options to generate the report with
         * @throws IOException if an I/O error occurs while generating the report
         */
        private void generateReport(OptionSet options) throws IOException {
			Log.info(this.serviceCapabilitiesReport.generate(options.valueOf(this.target)));
		}

		/**
         * Generates a project based on the provided options.
         * 
         * @param options the options for project generation
         * @throws IOException if an I/O error occurs during project generation
         */
        protected void generateProject(OptionSet options) throws IOException {
			ProjectGenerationRequest request = createProjectGenerationRequest(options);
			this.projectGenerator.generateProject(request, options.has(this.force));
		}

		/**
         * Creates a ProjectGenerationRequest object based on the provided options.
         * 
         * @param options the OptionSet containing the command line options
         * @return the created ProjectGenerationRequest object
         */
        protected ProjectGenerationRequest createProjectGenerationRequest(OptionSet options) {
			List<?> nonOptionArguments = new ArrayList<Object>(options.nonOptionArguments());
			Assert.isTrue(nonOptionArguments.size() <= 1, "Only the target location may be specified");
			ProjectGenerationRequest request = new ProjectGenerationRequest();
			request.setServiceUrl(options.valueOf(this.target));
			if (options.has(this.bootVersion)) {
				request.setBootVersion(options.valueOf(this.bootVersion));
			}
			if (options.has(this.dependencies)) {
				for (String dep : options.valueOf(this.dependencies).split(",")) {
					request.getDependencies().add(dep.trim());
				}
			}
			if (options.has(this.javaVersion)) {
				request.setJavaVersion(options.valueOf(this.javaVersion));
			}
			if (options.has(this.packageName)) {
				request.setPackageName(options.valueOf(this.packageName));
			}
			request.setBuild(options.valueOf(this.build));
			request.setFormat(options.valueOf(this.format));
			request.setDetectType(options.has(this.build) || options.has(this.format));
			if (options.has(this.type)) {
				request.setType(options.valueOf(this.type));
			}
			if (options.has(this.packaging)) {
				request.setPackaging(options.valueOf(this.packaging));
			}
			if (options.has(this.language)) {
				request.setLanguage(options.valueOf(this.language));
			}
			if (options.has(this.groupId)) {
				request.setGroupId(options.valueOf(this.groupId));
			}
			if (options.has(this.artifactId)) {
				request.setArtifactId(options.valueOf(this.artifactId));
			}
			if (options.has(this.name)) {
				request.setName(options.valueOf(this.name));
			}
			if (options.has(this.version)) {
				request.setVersion(options.valueOf(this.version));
			}
			if (options.has(this.description)) {
				request.setDescription(options.valueOf(this.description));
			}
			request.setExtract(options.has(this.extract));
			if (nonOptionArguments.size() == 1) {
				String output = (String) nonOptionArguments.get(0);
				request.setOutput(output);
			}
			return request;
		}

		/**
         * Processes the given argument by checking if it matches any of the options in the CAMEL_CASE_OPTIONS map.
         * If a match is found, the corresponding value is appended to the argument and returned.
         * If no match is found, the original argument is returned.
         *
         * @param argument the argument to be processed
         * @return the processed argument with the corresponding value appended, if a match is found; otherwise, the original argument
         */
        private static String processArgument(String argument) {
			for (Map.Entry<String, String> entry : CAMEL_CASE_OPTIONS.entrySet()) {
				String name = entry.getKey();
				if (argument.startsWith(name + " ") || argument.startsWith(name + "=")) {
					return entry.getValue() + argument.substring(name.length());
				}
			}
			return argument;
		}

	}

}
