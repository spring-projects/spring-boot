/*
 * Copyright 2012-2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.cli.command.init;

import java.io.IOException;
import java.util.Arrays;

import joptsimple.OptionSet;
import joptsimple.OptionSpec;

import org.springframework.boot.cli.command.Command;
import org.springframework.boot.cli.command.OptionParsingCommand;
import org.springframework.boot.cli.command.options.OptionHandler;
import org.springframework.boot.cli.command.status.ExitStatus;
import org.springframework.boot.cli.util.Log;

/**
 * {@link Command} that initializes a project using Spring initializr.
 *
 * @author Stephane Nicoll
 * @since 1.2.0
 */
public class InitCommand extends OptionParsingCommand {

	public InitCommand() {
		this(new InitOptionHandler(new InitializrService()));
	}

	public InitCommand(InitOptionHandler handler) {
		super("init", "Initialize a new project using Spring "
				+ "Initialzr (start.spring.io)", handler);
	}

	static class InitOptionHandler extends OptionHandler {

		private final ServiceCapabilitiesReportGenerator serviceCapabilitiesReport;

		private final ProjectGenerator projectGenerator;

		private OptionSpec<String> target;

		private OptionSpec<Void> listCapabilities;

		private OptionSpec<String> bootVersion;

		private OptionSpec<String> dependencies;

		private OptionSpec<String> javaVersion;

		private OptionSpec<String> packaging;

		private OptionSpec<String> build;

		private OptionSpec<String> format;

		private OptionSpec<String> type;

		private OptionSpec<Void> extract;

		private OptionSpec<Void> force;

		private OptionSpec<String> output;

		InitOptionHandler(InitializrService initializrService) {
			this.serviceCapabilitiesReport = new ServiceCapabilitiesReportGenerator(
					initializrService);
			this.projectGenerator = new ProjectGenerator(initializrService);

		}

		@Override
		protected void options() {
			this.target = option(Arrays.asList("target"), "URL of the service to use")
					.withRequiredArg().defaultsTo(
							ProjectGenerationRequest.DEFAULT_SERVICE_URL);
			this.listCapabilities = option(Arrays.asList("list", "l"),
					"List the capabilities of the service. Use it to discover the "
							+ "dependencies and the types that are available");
			projectGenerationOptions();
			otherOptions();
		}

		private void projectGenerationOptions() {
			this.bootVersion = option(Arrays.asList("boot-version", "b"),
					"Spring Boot version to use (for example '1.2.0.RELEASE')")
					.withRequiredArg();
			this.dependencies = option(
					Arrays.asList("dependencies", "d"),
					"Comma separated list of dependencies to include in the "
							+ "generated project").withRequiredArg();
			this.javaVersion = option(Arrays.asList("java-version", "j"),
					"Java version to use (for example '1.8')").withRequiredArg();
			this.packaging = option(Arrays.asList("packaging", "p"),
					"Packaging type to use (for example 'jar')").withRequiredArg();
			this.build = option("build",
					"The build system to use (for example 'maven' or 'gradle')")
					.withRequiredArg().defaultsTo("maven");
			this.format = option(
					"format",
					"The format of the generated content (for example 'build' for a build file, "
							+ "'project' for a project archive)").withRequiredArg()
					.defaultsTo("project");
			this.type = option(
					Arrays.asList("type", "t"),
					"The project type to use. Not normally needed if you use --build "
							+ "and/or --format. Check the capabilities of the service "
							+ "(--list) for more details").withRequiredArg();
		}

		private void otherOptions() {
			this.extract = option(Arrays.asList("extract", "x"),
					"Extract the project archive");
			this.force = option(Arrays.asList("force", "f"),
					"Force overwrite of existing files");
			this.output = option(
					Arrays.asList("output", "o"),
					"Location of the generated project. Can be an absolute or a "
							+ "relative reference and should refer to a directory when "
							+ "--extract is used").withRequiredArg();
		}

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

		private void generateReport(OptionSet options) throws IOException {
			Log.info(this.serviceCapabilitiesReport.generate(options.valueOf(this.target)));
		}

		protected void generateProject(OptionSet options) throws IOException {
			ProjectGenerationRequest request = createProjectGenerationRequest(options);
			this.projectGenerator.generateProject(request, options.has(this.force),
					options.has(this.extract), options.valueOf(this.output));
		}

		protected ProjectGenerationRequest createProjectGenerationRequest(
				OptionSet options) {
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
			if (options.has(this.packaging)) {
				request.setPackaging(options.valueOf(this.packaging));
			}
			request.setBuild(options.valueOf(this.build));
			request.setFormat(options.valueOf(this.format));
			request.setDetectType(options.has(this.build) || options.has(this.format));
			if (options.has(this.type)) {
				request.setType(options.valueOf(this.type));
			}
			if (options.has(this.output)) {
				request.setOutput(options.valueOf(this.output));
			}
			return request;
		}

	}

}
