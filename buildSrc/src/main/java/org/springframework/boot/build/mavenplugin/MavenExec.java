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

package org.springframework.boot.build.mavenplugin;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.JavaExec;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.api.tasks.TaskExecutionException;
import org.gradle.process.internal.ExecException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A custom {@link JavaExec} {@link Task task} for running Maven.
 *
 * @author Andy Wilkinson
 */
public class MavenExec extends JavaExec {

	private final Logger log = LoggerFactory.getLogger(MavenExec.class);

	private File projectDir;

	/**
	 * Constructor for MavenExec class. Sets the classpath using the Maven configuration
	 * of the project. Sets the command line arguments to "--batch-mode". Sets the main
	 * class to "org.apache.maven.cli.MavenCli".
	 */
	public MavenExec() {
		setClasspath(mavenConfiguration(getProject()));
		args("--batch-mode");
		getMainClass().set("org.apache.maven.cli.MavenCli");
	}

	/**
	 * Sets the project directory for the MavenExec class.
	 * @param projectDir the project directory to be set
	 */
	public void setProjectDir(File projectDir) {
		this.projectDir = projectDir;
		getInputs().file(new File(projectDir, "pom.xml"))
			.withPathSensitivity(PathSensitivity.RELATIVE)
			.withPropertyName("pom");
	}

	/**
	 * Executes the Maven command with the specified arguments. Sets the working directory
	 * and system property for the Maven command. Logs the output of the command to a
	 * temporary log file. If the log level is set to INFO, the log file contents are also
	 * printed to the console.
	 * @throws ExecException if an exception occurs during command execution
	 * @throws TaskExecutionException if an exception occurs while creating the log file
	 */
	@Override
	public void exec() {
		workingDir(this.projectDir);
		systemProperty("maven.multiModuleProjectDirectory", this.projectDir.getAbsolutePath());
		try {
			Path logFile = Files.createTempFile(getName(), ".log");
			try {
				args("--log-file", logFile.toFile().getAbsolutePath());
				super.exec();
				if (this.log.isInfoEnabled()) {
					Files.readAllLines(logFile).forEach(this.log::info);
				}
			}
			catch (ExecException ex) {
				System.out.println("Exec exception! Dumping log");
				Files.readAllLines(logFile).forEach(System.out::println);
				throw ex;
			}
		}
		catch (IOException ex) {
			throw new TaskExecutionException(this, ex);
		}
	}

	/**
	 * Returns the Maven configuration for the given project. If the configuration already
	 * exists, it is returned. Otherwise, a new configuration is created with the
	 * necessary dependencies.
	 * @param project the project for which to retrieve the Maven configuration
	 * @return the Maven configuration for the project
	 */
	private Configuration mavenConfiguration(Project project) {
		Configuration existing = project.getConfigurations().findByName("maven");
		if (existing != null) {
			return existing;
		}
		return project.getConfigurations().create("maven", (maven) -> {
			maven.getDependencies().add(project.getDependencies().create("org.apache.maven:maven-embedder:3.6.3"));
			maven.getDependencies().add(project.getDependencies().create("org.apache.maven:maven-compat:3.6.3"));
			maven.getDependencies().add(project.getDependencies().create("org.slf4j:slf4j-simple:1.7.5"));
			maven.getDependencies()
				.add(project.getDependencies()
					.create("org.apache.maven.resolver:maven-resolver-connector-basic:1.4.1"));
			maven.getDependencies()
				.add(project.getDependencies().create("org.apache.maven.resolver:maven-resolver-transport-http:1.4.1"));
		});
	}

	/**
	 * Returns the project directory.
	 * @return the project directory
	 */
	@Internal
	public File getProjectDir() {
		return this.projectDir;
	}

}
