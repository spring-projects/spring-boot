/*
 * Copyright 2012-2021 the original author or authors.
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

package org.springframework.boot.maven;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Execute;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.springframework.boot.loader.tools.MainClassFinder;

import java.io.File;
import java.io.IOException;

/**
 * Help find main class and set it to the specified property.
 *
 * @author Phillip Webb
 * @author Dave Syer
 * @author Stephane Nicoll
 * @author Björn Lindström
 * @author Scott Frederick
 * @author kerwin612
 * @since 1.0.0
 */
@Mojo(name = "fmc", defaultPhase = LifecyclePhase.PREPARE_PACKAGE, requiresProject = true, threadSafe = true,
		requiresDependencyResolution = ResolutionScope.RUNTIME)
@Execute(phase = LifecyclePhase.PREPARE_PACKAGE)
public class FindMainClassMojo extends AbstractDependencyFilterMojo {

	private static final String SPRING_BOOT_APPLICATION_CLASS_NAME = "org.springframework.boot.autoconfigure.SpringBootApplication";

	/**
	 * The name of the main class. If not specified the first compiled class found that
	 * contains a 'main' method will be used.
	 * @since 1.0.0
	 */
	@Parameter(property = "spring-boot.run.main-class")
	private String mainClass;

	/**
	 * Directory containing the classes and resource files that should be packaged into
	 * the archive.
	 * @since 1.0.0
	 */
	@Parameter(defaultValue = "${project.build.outputDirectory}", required = true)
	private File classesDirectory;

	@Parameter(defaultValue = "mainClass", required = true)
	private String mainClassPropertyName;

	@Parameter(defaultValue = "${project}", readonly = true, required = true)
	private MavenProject project;

	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {
		String mainClass = getStartClass();
		if (mainClass == null) {
			throw new MojoExecutionException("Unable to find a suitable main class");
		}
		getLog().info("found main class: " + mainClass);
		project.getProperties().put(mainClassPropertyName, mainClass);
	}

	protected String getStartClass() throws MojoExecutionException {
		String mainClass = this.mainClass;
		if (mainClass == null) {
			try {
				mainClass = MainClassFinder.findSingleMainClass(this.classesDirectory,
						SPRING_BOOT_APPLICATION_CLASS_NAME);
			}
			catch (IOException ex) {
				throw new MojoExecutionException(ex.getMessage(), ex);
			}
		}
		if (mainClass == null) {
			throw new MojoExecutionException("Unable to find a suitable main class, please add a 'mainClass' property");
		}
		return mainClass;
	}

}
